package com.example.nexus.core.tool.tools

import android.content.Context
import com.example.nexus.core.policy.RiskLevel
import com.example.nexus.core.tool.AgentTool
import com.example.nexus.core.tool.ToolContext
import com.example.nexus.core.tool.ToolParameter
import com.example.nexus.core.tool.ToolResult
import com.example.nexus.core.tool.ToolSchema
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Sandboxed file helper ensuring path safety and preventing directory traversal attacks.
 */
object FileSandboxHelper {
    fun getSandboxDir(context: Context): File {
        val dir = File(context.filesDir, "sandbox")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun resolveSafeFile(context: Context, relativePath: String): File {
        val sandbox = getSandboxDir(context).canonicalFile
        val sanitized = relativePath.trim().removePrefix("/").replace("\\", "/")
        val target = File(sandbox, sanitized).canonicalFile
        if (!target.path.startsWith(sandbox.path)) {
            throw SecurityException("ACCESS_DENIED: Path traversal attempt outside sandbox directory: '$relativePath'")
        }
        return target
    }
}

class FileListTool(private val context: Context) : AgentTool {
    override val id: String = "file.list"
    override val name: String = "List Sandboxed Files"
    override val description: String = "Lists files and subdirectories stored in the secure NEXUS sandbox environment."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "Relative path within sandbox (defaults to root '.')",
                required = false
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val relPath = arguments.optString("path", ".").ifBlank { "." }
        return try {
            val dir = FileSandboxHelper.resolveSafeFile(this.context, relPath)
            if (!dir.exists()) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "DIRECTORY_NOT_FOUND: Path '$relPath' does not exist in sandbox",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            if (!dir.isDirectory) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "NOT_A_DIRECTORY: '$relPath' is a file, not a directory",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }

            val files = dir.listFiles() ?: emptyArray()
            val array = JSONArray()
            for (f in files.sortedBy { it.name }) {
                val obj = JSONObject().apply {
                    put("name", f.name)
                    put("isDirectory", f.isDirectory)
                    put("sizeBytes", if (f.isFile) f.length() else 0L)
                    put("lastModified", f.lastModified())
                }
                array.put(obj)
            }

            ToolResult(
                success = true,
                output = JSONObject().apply {
                    put("path", relPath)
                    put("fileCount", files.size)
                    put("files", array)
                }.toString(2),
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to list files",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class FileReadTool(private val context: Context) : AgentTool {
    override val id: String = "file.read"
    override val name: String = "Read File"
    override val description: String = "Reads text contents of a file inside the secure NEXUS sandbox."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "Relative path of file to read in sandbox",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.LOW

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val path = arguments.optString("path", "").trim()
        if (path.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "PATH_REQUIRED: Missing 'path' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val file = FileSandboxHelper.resolveSafeFile(this.context, path)
            if (!file.exists() || !file.isFile) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "FILE_NOT_FOUND: File '$path' does not exist in sandbox",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            if (file.length() > 500 * 1024) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "FILE_TOO_LARGE: File exceeds maximum safe read limit (500KB)",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            val content = file.readText()
            ToolResult(
                success = true,
                output = content,
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to read file",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class FileCreateTool(private val context: Context) : AgentTool {
    override val id: String = "file.create"
    override val name: String = "Create or Write File"
    override val description: String = "Creates or overwrites a text file with specified content in the sandbox."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "Filename or relative path inside sandbox",
                required = true
            ),
            ToolParameter(
                name = "content",
                type = "string",
                description = "Text content to write into file",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val path = arguments.optString("path", "").trim()
        val content = arguments.optString("content", "")
        if (path.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "PATH_REQUIRED: Missing 'path' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val file = FileSandboxHelper.resolveSafeFile(this.context, path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            ToolResult(
                success = true,
                output = "File '$path' created successfully (${content.length} characters written).",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to write file",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class FileCopyTool(private val context: Context) : AgentTool {
    override val id: String = "file.copy"
    override val name: String = "Copy File"
    override val description: String = "Copies an existing file to a destination path inside the sandbox."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter("source", "string", "Source file path", required = true),
            ToolParameter("destination", "string", "Destination file path", required = true)
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val sourcePath = arguments.optString("source", "").trim()
        val destPath = arguments.optString("destination", "").trim()
        if (sourcePath.isBlank() || destPath.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "ARGUMENTS_MISSING: Both 'source' and 'destination' are required",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val src = FileSandboxHelper.resolveSafeFile(this.context, sourcePath)
            val dest = FileSandboxHelper.resolveSafeFile(this.context, destPath)
            if (!src.exists()) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "SOURCE_NOT_FOUND: Source file '$sourcePath' does not exist",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            dest.parentFile?.mkdirs()
            src.copyTo(dest, overwrite = true)
            ToolResult(
                success = true,
                output = "Copied '$sourcePath' to '$destPath' successfully.",
                executionTimeMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to copy file",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class FileMoveTool(private val context: Context) : AgentTool {
    override val id: String = "file.move"
    override val name: String = "Move or Rename File"
    override val description: String = "Moves or renames a file inside the sandbox."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter("source", "string", "Source file path", required = true),
            ToolParameter("destination", "string", "Destination file path", required = true)
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.MEDIUM

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val sourcePath = arguments.optString("source", "").trim()
        val destPath = arguments.optString("destination", "").trim()
        if (sourcePath.isBlank() || destPath.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "ARGUMENTS_MISSING: Both 'source' and 'destination' are required",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val src = FileSandboxHelper.resolveSafeFile(this.context, sourcePath)
            val dest = FileSandboxHelper.resolveSafeFile(this.context, destPath)
            if (!src.exists()) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "SOURCE_NOT_FOUND: Source file '$sourcePath' does not exist",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            dest.parentFile?.mkdirs()
            val moved = src.renameTo(dest)
            if (moved) {
                ToolResult(
                    success = true,
                    output = "Moved '$sourcePath' to '$destPath' successfully.",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            } else {
                src.copyTo(dest, overwrite = true)
                src.delete()
                ToolResult(
                    success = true,
                    output = "Moved '$sourcePath' to '$destPath' successfully.",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to move file",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}

class FileDeleteTool(private val context: Context) : AgentTool {
    override val id: String = "file.delete"
    override val name: String = "Delete File"
    override val description: String = "Deletes a file from the sandbox. DESTRUCTIVE ACTION: Always requires explicit user confirmation."
    override val argumentSchema: ToolSchema = ToolSchema(
        listOf(
            ToolParameter(
                name = "path",
                type = "string",
                description = "Relative path of file to permanently delete",
                required = true
            )
        )
    )
    override val requiredPermissions: List<String> = emptyList()
    override val riskLevel: RiskLevel = RiskLevel.HIGH

    override suspend fun execute(arguments: JSONObject, context: ToolContext): ToolResult {
        val start = System.currentTimeMillis()
        val path = arguments.optString("path", "").trim()
        if (path.isBlank()) {
            return ToolResult(
                success = false,
                output = "",
                error = "PATH_REQUIRED: Missing 'path' parameter",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }

        return try {
            val file = FileSandboxHelper.resolveSafeFile(this.context, path)
            if (!file.exists()) {
                return ToolResult(
                    success = false,
                    output = "",
                    error = "FILE_NOT_FOUND: File '$path' does not exist in sandbox",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
            val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
            if (deleted) {
                ToolResult(
                    success = true,
                    output = "Deleted '$path' permanently from sandbox.",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            } else {
                ToolResult(
                    success = false,
                    output = "",
                    error = "DELETE_FAILED: Could not delete file '$path'",
                    executionTimeMs = System.currentTimeMillis() - start
                )
            }
        } catch (e: Exception) {
            ToolResult(
                success = false,
                output = "",
                error = e.localizedMessage ?: "Failed to delete file",
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }
}
