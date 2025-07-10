# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial implementation of Claude Code SDK for JVM
- Complete type system with sealed interfaces for messages
- Process-based transport for CLI communication
- JSON message parser using kotlinx.serialization
- Coroutine-based streaming API with Flow
- Java-friendly API with CompletableFuture and callbacks
- DSL builder for query construction
- Extension functions for message filtering and processing
- Cross-platform CLI detection (Windows, macOS, Linux)
- Comprehensive test suite with 80% code coverage
- Timeout mechanism with configurable duration
- Full API documentation with Dokka
- Builder pattern for Java compatibility
- Support for all Claude Code CLI options
- MCP server configuration support
- Exception hierarchy for proper error handling

### Technical Details
- Kotlin 2.2.0 with explicit API mode
- Java 8+ compatibility
- Dependencies: kotlinx-coroutines, kotlinx-serialization, okio
- JaCoCo for code coverage reporting

## [0.1.0] - TBD

Initial release (placeholder for first version)