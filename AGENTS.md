# AGENT WORKFLOW & PROTOCOLS

> **IMPORTANT**: This file acts as the primary entry point for AI Agents working on this project.
> You are required to strictly adhere to the file structure and documentation standards defined below.

## 1. Documentation Standards

When writing logs, plans, or reviews in this project, you must follow these style guidelines:

* **No Emojis**: Do not use emojis in any markdown files. Maintain a strictly professional and technical tone.
* **High Readability**:
    * Use **Bold** text to highlight key entities (filenames, function names, critical statuses).
    * Use **Lists** (bullet points) to break down complex information.
    * Ensure proper **Line Breaks** between sections to prevent "walls of text."
    * Use **Code Blocks** for file paths, commands, and code snippets.
* **Atomic Updates**: When updating logs, append new information without deleting past context unless explicitly instructed.

---

## 2. Directory Structure & Responsibilities

The agent workspace is isolated in the `.agents/` directory. Do not store context in the root directory.

| Directory | Access | Purpose |
| :--- | :--- | :--- |
| **`.agents/context/`** | **Read-Only** | Project goals, tech stack, and architectural constraints. |
| **`.agents/inspiration/`** | **Read-Only** | Design inspirations and reference docs (**Human Developers Only**). |
| **`.agents/work/`** | **Read/Write** | Unified task documents using the plan style, with history appended in the same file. (Subdirs: **`backlog/`**, **`completed/`**, **`on-hold/`**) |
| **`.agents/review/`** | **Write** | Self-review notes, test results, and QA logs before completion. (Subdirs: **`completed/`**, **`on-hold/`**) |
| **`.agents/issue/`** | **Write** | Detailed analysis of bugs, errors, or blockers encountered. (Subdirs: **`completed/`**, **`on-hold/`**) |

---

## 3. Mandatory Protocols

### ① Inspiration Directory Restriction
The `.agents/inspiration` directory is reserved for human developers. **Agents are strictly forbidden from modifying, deleting, or adding files in this directory.** Agents may only read these files to gain context.

### ② File Lifecycle (The `completed/` & `on-hold/` Rules)
To maintain a clean workspace, once a task, issue, or review is finalized or suspended:
1.  Mark the internal status as **Completed**, **Closed**, or **On Hold**.
2.  Move the file to a subdirectory within its respective parent:
    *   **`completed/`**: For fully finished tasks.
    *   **`on-hold/`**: For suspended, deferred, or deprioritized tasks.

### ③ Structural Templates
Agents must follow these structures for consistency:

#### **Work Document (`.agents/work/`)**
*   **Header**: `# Plan: [Title]`
*   **Metadata**: Status, Date, Goal (Concise summary of the objective).
*   **Parent Task**: Optional reference to the originating task document when this work is derived from a prior task.
*   **Body**: Breakdown into **Phases** or **Steps** using checklists (`- [ ]`).
*   **History**: Append entries under `## History` at the end of the file.

#### **Issue Document (`.agents/issue/`)**
*   **Header**: `# Issue: [Short Description]`
*   **Metadata**: Severity (Critical/High/Low), Status, Date.
*   **Body**: **Description** (Problem), **Affected Files**, **Findings**, and **Recommendation** (How to fix).

#### **Review Document (`.agents/review/`)**
*   **Header**: `# [Type] Review - [Date]`
*   **Metadata**: Reviewer, Scope (e.g., Security, Logic).
*   **Body**: **Summary**, **Findings** (Detailed analysis), and **Action Items**.

---

## 4. Standard Operational Procedure

### Phase 1: Analysis & Setup
1.  **Read Context**: Review `AGENTS.md` and `.agents/context/` to align with the project scope.
2.  **Select Task**: Identify the next item from `.agents/work/backlog/`.
3.  **Initialize Workspace`:
    * Create a new file in `.agents/work/` (e.g., `.agents/work/task_name.md`) using the **Work Document** template.
    * Use this file to draft your implementation plan before writing code.
    * If this task is derived from a prior task, include **Parent Task** with the path to the originating work document.

### Phase 0: Conversational Tasks Exception
For purely conversational tasks that do not require writing documents or modifying files, skip creating or updating any `.agents/` work, review, or issue documents.

### Phase 2: Execution
1.  **Implement**: Write the code in the project source directories.
2.  **Update Context**: Continuously update your status in the `.agents/work/` file.

### Phase 3: Verification & Closure
1.  **Self-Review**: Create a summary in `.agents/review/` verifying that requirements are met.
2.  **Log History**: Append a summary of the completed task under `## History` in the task's **Work Document**.
    * *Format*: Timestamp, Task Name, Summary of Changes, Modified Files.
3.  **Update Status**: Mark the task as **Completed**, then move the file to `.agents/work/completed/`.
4.  **Cleanup**: (Optional) Move the temporary `.agents/work/` file to an archive or follow the **File Lifecycle Rule`.

## 5. Git Commit Standards
When committing changes, follow these guidelines:
* **prefix**: Start the commit message with "{type}. " (e.g., "feat. ", "fix. ", "docs. ").
* **Types**:
    * `feat.`: New feature
    * `fix.`: Bug fix
    * `docs.`: Documentation changes
    * `style.`: Code style changes (formatting, missing semi-colons, etc.)
    * `refactor.`: Code refactoring
    * `test.`: Adding or updating tests
    * `chore.`: Maintenance tasks (build process, dependencies, etc.)
* **Atomic Commits**: Each commit should represent a single logical change.
* **Descriptive Messages**: Use clear, concise commit messages that describe the change.
