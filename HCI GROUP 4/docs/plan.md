# PULS3122 Coursework Development Plan (Compressed 1-Week Schedule)

**Note:** This plan outlines an extremely aggressive 1-week schedule. It requires intense focus, parallel work, and significant simplification compared to the original brief's ideal process. Scope reduction, particularly in user research, iterative design, and formal testing, is necessary. Meeting all original marking criteria perfectly under this constraint will be very difficult.

## Day 1: Setup, Analysis & Basic Design

*   **(AM) Setup & Planning:**
    *   Confirm team, set up communication channel (e.g., Slack/Discord).
    *   Create GitHub repo, agree on simple branching (e.g., feature branches -> main).
    *   Quickly confirm JDK/IDE setup.
    *   Assign initial high-level roles/areas (e.g., UI, 2D Graphics, 3D Graphics, File I/O).
    *   **Rapid Review:** Team reads coursework brief (`coursework.md`) and Appendix A together.
*   **(PM) Rapid Analysis & Requirements:**
    *   Focus *only* on Appendix A requirements. Skip external requirement gathering.
    *   Quickly define 1-2 core user stories for the designer. Skip detailed personas.
    *   Document *essential* requirements for task tracking.
*   **(PM) Basic Design:**
    *   **Skip Low-Fidelity Prototyping.**
    *   Go directly to collaborative sketching or a *very* basic digital prototype (e.g., Figma) for core screens (Login, Main Design View, Save/Load). Focus on layout and essential controls.
    *   **Skip initial user feedback.** Rely on team consensus.

## Day 2: Core Structure & UI Shell

*   **Project Setup:** Initialize Java project (Maven/Gradle recommended).
*   **Parallel Work:**
    *   **Team Member 1 (UI Lead):** Implement basic Java Swing window structure, navigation (if any), and placeholders for main components based on Day 1 design. Implement Login screen UI.
    *   **Team Member 2 (2D):** Research/set up basic 2D drawing library/API in Java Swing. Create a simple canvas panel.
    *   **Team Member 3 (3D):** Research/set up basic 3D visualization (placeholder or simple library integration).
    *   **Team Member 4 (Data):** Define simple data structures for designs, rooms, furniture. Plan basic file save/load mechanism (e.g., simple text or serialization).
    *   **Team Member(s) 5/6:** Support others, start documenting Day 1 activities for the report, refine task board.
*   **Integration:** Basic integration of UI shell and canvas panels.
*   **Version Control:** Frequent commits from all members.

## Day 3: Feature Implementation - Part 1

*   **Parallel Work (Focus on Core Functionality):**
    *   **UI:** Implement room parameter input fields and connect to data structures. Implement basic toolbar/menu actions.
    *   **2D:** Implement drawing basic shapes (representing furniture) on the 2D canvas. Implement selecting shapes.
    *   **3D:** Implement basic 3D view rendering based on the 2D layout (even if very simple cubes/shapes).
    *   **Data:** Implement basic "Save Design" functionality.
    *   **Documentation:** Continue documenting progress, design decisions, and implementation snippets.
*   **Integration & Team Check-in:** Ensure components are working together.

## Day 4: Feature Implementation - Part 2

*   **Parallel Work (Adding Detail):**
    *   **UI/2D:** Implement changing color of selected shapes/whole design. Implement scaling shapes/design.
    *   **3D:** Refine 3D view. Implement basic shading if feasible, otherwise skip.
    *   **Data:** Implement "Load Design" functionality. Implement "Edit/Delete Design" (basic file management or in-memory list).
    *   **Login:** Connect login UI to basic authentication logic (can be hardcoded/simple check initially).
    *   **Documentation:** Keep documenting.
*   **Integration & Code Review:** Review key parts of the code for obvious issues.

## Day 5: Refinement & Internal Testing

*   **Bug Fixing:** Address critical bugs found during development.
*   **Refinement:** Polish UI interactions where possible. Ensure core requirements from Appendix A are met functionally.
*   **Internal Testing:**
    *   **Skip formal user testing.**
    *   Team members test the application thoroughly, focusing on the main user stories (Create, View 2D/3D, Scale, Color, Save, Load).
    *   Log bugs/issues on the task board.
*   **Code Freeze (End of Day):** Aim to have core functionality stable.

## Day 6: Documentation & Video

*   **(AM) Final Bug Fixing:** Address critical bugs found during internal testing.
*   **(AM) GitHub:** Clean up repository, ensure `README.md` is present with basic instructions and credits (if any). Ensure history is reasonable (commits throughout the week).
*   **(PM) Report Writing:**
    *   Collaboratively write the report sections (Introduction, Background, Simplified Gathering Data, Design [show final UI], Implementation [screenshots, code links], Evaluation [describe *internal* testing], Summary, References).
    *   Focus on concisely describing what was done. Keep it close to 2000 words *if possible*, but prioritize clarity over length.
    *   Add GitHub link.
*   **(Late PM / Evening) Video Recording:**
    *   Plan a quick run-through: Demo login, creating a simple design, showing 2D/3D, color/scale, save/load.
    *   *Quickly* explain key code sections/design choices.
    *   Ensure all members appear briefly.
    *   Record in one or two takes, minimal editing. Ensure required format (MP4, 720p+).
    *   Upload to YouTube, get public link. Add link to report.

## Day 7: Final Checks & Submission

*   **(AM) Final Review:**
    *   Read through the report one last time.
    *   Check *all* links (GitHub, YouTube) are correct, public, and working.
    *   Verify the video meets length/format requirements.
    *   Confirm application runs.
*   **(Submission):** Submit the single PDF report via DLE well before the deadline.

## Timeline Overview (1 Week)

*   **Day 1:** Setup, Plan, Analyze, Basic Design
*   **Day 2:** Core Structure & UI Shell Implementation
*   **Day 3:** Feature Implementation (Part 1)
*   **Day 4:** Feature Implementation (Part 2)
*   **Day 5:** Refinement & Internal Testing
*   **Day 6:** Bug Fixing, Documentation (Report & GitHub), Video Recording/Upload
*   **Day 7:** Final Checks & Submission

This schedule is extremely demanding. Success depends on excellent teamwork, clear communication, rapid decision-making, and accepting necessary compromises on features and process formality. Good luck! 