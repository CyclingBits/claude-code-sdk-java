# Comprehensive Kotlin Code Review Prompt

## Context

This is a server‑side Kotlin project built with **Gradle** and is **publicly hosted on GitHub as the author's showcase**. Consequently, the codebase must be **maximally easy to understand**, **free of defects**, and **fully compliant with the latest language, framework, and industry standards**. All source code, configuration files and build scripts are in scope. *(Testing code is intentionally out of scope for this review.)*

Before commencing the review, prepare a concise action plan that breaks the process into small, manageable steps.

## Review Objectives

1. **Simplicity & Readability** – Assess whether each class, function and expression is as simple and self‑explanatory as reasonably possible.
2. **Naming Conventions** – Verify that packages, classes, functions, variables and Gradle modules follow modern Kotlin/JVM naming standards and convey clear intent.
3. **Idiomatic Kotlin** – Check adherence to the latest Kotlin Style Guide (2025), including:

    * Null‑safety and absence of !!
    * Preference for `val` over `var`, immutability
    * Extension functions, data classes, inline/value classes
    * Consistent coroutine usage and structured concurrency
4. **Design & Architecture** - Evaluate compliance with recognised patterns for JVM  (layered / hexagonal / clean architecture / SOLID principles).
5. **Project Structure & Build** – Inspect the Gradle layout, dependency management, module boundaries, and build plugins for clarity and maintainability.
6. **Performance & Concurrency** – Identify blocking calls inside coroutines, unnecessary reflection, excessive object allocation, or misuse of synchronized blocks.
7. **Duplication & Complexity** - Locate duplicated logic or deeply nested code and propose simplifications.
8. **Forward‑Looking Improvements** –Suggest adoption of upcoming Kotlin features (K2 compiler, context receivers, data objects).
9. **Dependencies & Versions** – Verify that all Gradle dependencies, plugins, and Docker base images are pinned to the latest stable release (or approved LTS) and flag any outdated or vulnerable components.
10. **Public Showcase Quality** – Ensure that README, documentation, and overall presentation reflect a polished portfolio‑grade project that invites external contributors and employers.

> **Testing‑related observations are out of scope** – lack of unit/integration tests should *not* be flagged as a defect.

## Deliverables

Produce a **structured Markdown report** with the following sections:

1. **Executive Summary** – Key takeaways in plain language (max 10 sentences).

2. **Findings by Severity** – Table or bullet list grouped into `Critical`, `Major`, `Minor`, `Informational`. Each finding must include:

    * *Location* (file & line or symbol name)
    * *Issue description* (concise, no fluff)
    * *Rationale* with citation to authoritative source (e.g. Kotlin Docs, Effective Kotlin, Spring Docs).

3. **Refactor & Fix Plan** – Prioritised action list, grouped by theme (Architecture, Language Features, Performance, Security, Readability). For each item indicate **Effort** (Low/Medium/High) and **Expected Impact** (Low/Medium/High).

4. **Appendix** – Optional code snippets illustrating before‑and‑after examples.

5. **Repository Placement** – Save the generated Markdown review report in the project’s root directory (next to the `pom.xml`).

## Tone & Style Guidelines

* **Brutally honest, zero sugar‑coating** – if something is dubious, say so plainly.
* **Skeptical & Questioning** – challenge design choices; ask "why" when patterns seem misapplied.
* **Forward‑Thinking** – suggest innovations that will pay off over the next 1‑3 years, not just today.
* Avoid re‑stating obvious style‑guide rules unless they are violated.


# **Use ultrathink mode !!!**
