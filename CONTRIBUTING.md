Contributing

The main branch is protected. You cannot push directly to main — all changes go through a Pull Request (PR) and need 1 approval before merging.

Workflow
1) Update your local main

bash
   git checkout main
   git pull

2) Create a branch for your work
bash
   git checkout -b your-name/short-description
 
3) Commit your changes
bash
   git add .
   git commit -m "Add notes on L1/L2/L3 cache"
   
4) Push your branch
bash
   git push -u origin your-name/short-description

Open a Pull Request on GitHub from your branch into main.

Ask a teammate to review. Once approved, merge the PR and delete the branch.
