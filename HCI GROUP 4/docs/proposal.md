# PULS3122 Furniture Design Application Proposal (1-Week Scope)

## Introduction

This document outlines the proposal for a desktop application designed for a furniture company's in-store designers, developed within an accelerated one-week timeframe as per the PULS3122 coursework requirements. The primary goal is to create a functional tool enabling designers to quickly visualize basic furniture layouts within customer-defined room parameters. Given the severe time constraint, this proposal focuses on delivering the core functionality outlined in Appendix A, necessarily simplifying or omitting aspects like extensive user research, iterative prototyping, and formal usability testing described in the full coursework brief.

## Functionality

The application will allow authenticated designers (via a simple login mechanism) to create and manage furniture layout designs. Designers will first define the basic parameters of a customer's room, including its dimensions (size and shape) and potentially a base color scheme. Within this defined room context, designers can add predefined 2D shapes representing common furniture items (e.g., chairs, tables). These shapes can be positioned and potentially scaled within the 2D representation of the room. The core visualization features will include rendering the 2D layout and providing a basic 3D perspective view of the arrangement. Designers will be able to apply color changes to the entire design or selected furniture items. A simple shading effect might be implemented in the 3D view if time permits, but is a secondary goal. Essential data management features will include the ability to save the current design layout to a local file and subsequently load saved designs for review or modification. Basic functionality to delete or manage saved designs will also be included.

## User Interface Pages/Screens

The user interface will be kept straightforward to facilitate rapid development. It will primarily consist of:

1.  **Login Screen:** A simple screen requiring designer credentials for access. For this scope, authentication might be basic (e.g., hardcoded credentials).
2.  **Main Design Workspace:** This central screen will feature a primary canvas for 2D layout design, input fields/controls for defining room parameters (size, shape, color), toolbars or menus for adding furniture shapes, selecting items, applying color/scaling, toggling between 2D and 3D views, and buttons for saving/loading/deleting designs. A secondary panel or window will display the 3D visualization.

## Technical Details

The application will be developed using the **Java programming language** and the **Java Swing** toolkit for the graphical user interface, as mandated by the coursework brief. A standard integrated Java graphics library (e.g., `java.awt.Graphics2D`) will be used for rendering the 2D shapes and managing the design canvas. For the 3D visualization, a basic approach using standard Java libraries or potentially a very lightweight, easily integrated 3D library will be employed, focusing on simple perspective projection rather than complex rendering. The project structure will likely follow a simple pattern (e.g., separating UI, data model, and basic logic). Design persistence will be handled through simple file I/O, potentially serializing design data objects or using a basic text format (like CSV or JSON) for saving and loading layouts locally. Version control will be managed using **Git**, with the codebase hosted on **GitHub**.

## Non-Technical Details

The **target user** remains the in-store furniture designer. Due to the 1-week constraint, the **development process** will follow a highly condensed agile approach, focusing on rapid implementation of core features identified in Appendix A. Formal requirements gathering beyond Appendix A, persona development, multiple prototyping stages (low/high fidelity), and external user testing (formative/summative) will be omitted. **Evaluation** will rely solely on internal testing by the development team to ensure core functionality operates as expected. **Deliverables** will include the functional Java application, a GitHub repository containing the source code (with commit history reflecting the week's work and a basic README), a concise final report (approx. 2000 words where feasible) documenting the streamlined process and implementation, and a short YouTube video (7-12 mins) demonstrating the application and briefly explaining the code and design choices. 

## Payments and Deliverables

**Cost and Payment Terms:** The total cost for the development of this furniture design application, encompassing all deliverables outlined below, is Rs. 30,000/=. To initiate the project, an advance payment of 50% (Rs. 15,000/=) of the total cost is required. The remaining 50% will be due upon final delivery of the software and documentation.

**Deliverables:** Upon completion and final payment, the following items will be delivered:
1.  The complete source code for the Java application, managed within a Git repository.
2.  A compiled, executable version of the desktop application.
3.  The final project report (as described in the Non-Technical Details section) documenting the development process, design, implementation, and internal evaluation.

**Intellectual Property:** Upon receipt of the final payment, all intellectual property rights for the custom-developed source code and the application described in this proposal will be transferred to the client (the furniture company). The development team retains the right to use the underlying technologies and general knowledge gained during the project for future endeavors. 