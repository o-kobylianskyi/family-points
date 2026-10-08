@echo off
cd /d C:\Users\Oleh\IdeaProjects\family-points\frontend

if not exist node_modules (
    echo Installing dependencies...
    call npm install
)

echo Starting frontend...
call npm run dev

pause