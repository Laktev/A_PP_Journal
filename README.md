# jEntries
Submitted in fulfillment of the SUMMATIVE ASSESSMENT requirement for: **LICT 224 | Programming Fundamentals**

**View GitHub Repositories via this link**
https://github.com/Laktev/A_PP_Journal

(Release): https://github.com/Laktev/A_PP_Journal/releases/edit/V1.0

---

## Submitted By:

- ACOPIADO, Nhel Jane D.
- CAÑON, Crisny John
- SOBERANO, Nicole T.
- TIMA, Abegeil M.
- VILLAFLOR, Emmanuel Jr. L.

**Year Level:**
2nd Year (A.Y. 2025–2026)

---

## Project Information

**Program:**  
Bachelor of Library and Information Science

**Faculty:**  
Hermoso J. Tupas Jr.

**Schedule:**  
Mon · Wed | 3:30 PM – 6:00 PM (IT 306)

**Date Given:**  
April 30, 2026

**Due Date:**  
May 29, 2026

---

## Purpose

This project was developed to provide a secure, lightweight desktop journal application for users who need a private and organized way to record personal reflections, daily logs, and notes. The primary objective is to address the problem of data exposure and disorganization that commonly occurs with cloud-based or unencrypted note-taking tools. By keeping all data local by default, the application ensures that users retain full control over their personal entries.

The system was designed for individuals such as students, professionals, and researchers who require a self-contained local application to write, format, and safely preserve journal entries without depending on constant internet access. The focus is on providing a distraction-free environment where users can document their thoughts with confidence that their information remains private and protected.

Core functionality includes a rich text editor with basic document styling, multi-user login profiles, chronological sorting of entries, and automatic timestamps for both creation and last-edited tracking. To support data integrity and accessibility, the application also integrates background synchronization with Google Drive via its API, allowing users to maintain an off-site backup while keeping their primary data stored locally.

---

## Scope

The scope of this project covers the development of a Java Swing desktop application that operates entirely offline for its core writing and management tasks. Multi-user login is implemented using SHA-256 password hashing to separate and secure individual profiles, with each user's entries stored in isolated local directories. The application supports document styling options, including bold, italics, underline, and strikethrough. Entries are automatically timestamped and sorted chronologically for easier navigation, and XML files are used for data persistence of both user account information and journal entries.

The project also includes background synchronization with Google Drive to provide interoperability and backup without compromising the local-first design. The system is limited to desktop environments that support the Java Runtime Environment and does not include mobile or web versions. Formatting features such as numbered lists, indentation, text alignment, and undo/redo are currently under development and may have partial or inconsistent functionality in this release. Google Drive synchronization requires an active internet connection, and the application does not support real-time collaboration or shared entries, as it is designed exclusively for single-user private journaling.

---

### How to Run
1. Download and extract `JEntries-v1.0.zip`
2. Run `JEntries.exe` — no Java installation required, JRE is bundled
3. Create an account on first launch and start journaling

---

### Requirements
- Windows (64-bit)
- Internet connection only required for Google Drive sync
