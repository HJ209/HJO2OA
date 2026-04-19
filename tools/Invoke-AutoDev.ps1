[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('summary', 'next', 'show', 'prompt', 'verify')]
    [string]$Command,

    [string]$TaskId,

    [int]$Limit = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-RepoRoot {
    return (Split-Path -Parent $PSScriptRoot)
}

function Get-CatalogPath {
    return (Join-Path $PSScriptRoot 'auto-dev.tasks.json')
}

function Get-TaskCatalog {
    $catalogPath = Get-CatalogPath
    if (-not (Test-Path $catalogPath)) {
        throw "Task catalog not found: $catalogPath"
    }

    return (Get-Content -Raw -Encoding UTF8 $catalogPath | ConvertFrom-Json)
}

function Get-Tasks {
    param([object]$Catalog)

    return @($Catalog.tasks)
}

function Get-TaskIndex {
    param([object[]]$Tasks)

    $index = @{}
    foreach ($task in $Tasks) {
        $index[$task.id] = $task
    }
    return $index
}

function Get-PriorityRank {
    param([string]$Priority)

    switch ($Priority) {
        'P0' { return 0 }
        'P1' { return 1 }
        'P2' { return 2 }
        'P3' { return 3 }
        default { return 9 }
    }
}

function Get-TaskById {
    param(
        [object[]]$Tasks,
        [string]$Id
    )

    foreach ($task in $Tasks) {
        if ($task.id -eq $Id) {
            return $task
        }
    }

    return $null
}

function Get-DependencySummary {
    param(
        [object]$Task,
        [hashtable]$TaskIndex
    )

    $dependencies = @($Task.dependsOn)
    if ($dependencies.Count -eq 0) {
        return @('none')
    }

    $result = @()
    foreach ($dependencyId in $dependencies) {
        if ($TaskIndex.ContainsKey($dependencyId)) {
            $dependency = $TaskIndex[$dependencyId]
            $result += "$dependencyId ($($dependency.status))"
        } else {
            $result += "$dependencyId (missing)"
        }
    }
    return $result
}

function Test-TaskReady {
    param(
        [object]$Task,
        [hashtable]$TaskIndex
    )

    if ($Task.status -ne 'ready') {
        return $false
    }

    foreach ($dependencyId in @($Task.dependsOn)) {
        if (-not $TaskIndex.ContainsKey($dependencyId)) {
            return $false
        }

        if ($TaskIndex[$dependencyId].status -ne 'done') {
            return $false
        }
    }

    return $true
}

function Format-BulletList {
    param([object[]]$Values)

    return @($Values | ForEach-Object { "- $_" })
}

function Write-Summary {
    param([object]$Catalog)

    $tasks = Get-Tasks -Catalog $Catalog
    $taskIndex = Get-TaskIndex -Tasks $tasks
    $readyTasks = @(
        $tasks |
            Where-Object { Test-TaskReady -Task $_ -TaskIndex $taskIndex } |
            Sort-Object @{ Expression = { Get-PriorityRank $_.priority } }, id
    )

    $lines = @()
    $lines += "Project: $($Catalog.metadata.projectName)"
    $lines += "Stage: $($Catalog.metadata.stage)"
    $lines += "Snapshot: $($Catalog.metadata.snapshotDate)"
    $lines += ''
    $lines += 'Current assessment:'
    $lines += Format-BulletList -Values @($Catalog.metadata.summaryBullets)
    $lines += ''
    $lines += "Task count: $($tasks.Count)"
    $lines += "Ready now: $($readyTasks.Count)"

    if ($readyTasks.Count -gt 0) {
        $lines += ''
        $lines += 'Recommended next:'
        foreach ($task in ($readyTasks | Select-Object -First 3)) {
            $lines += "- [$($task.priority)] $($task.id) $($task.title)"
        }
    }

    return ($lines -join [Environment]::NewLine)
}

function Write-NextTasks {
    param(
        [object]$Catalog,
        [int]$MaxCount
    )

    $tasks = Get-Tasks -Catalog $Catalog
    $taskIndex = Get-TaskIndex -Tasks $tasks
    $readyTasks = @(
        $tasks |
            Where-Object { Test-TaskReady -Task $_ -TaskIndex $taskIndex } |
            Sort-Object @{ Expression = { Get-PriorityRank $_.priority } }, id
    )

    if ($readyTasks.Count -eq 0) {
        return 'No ready tasks with satisfied dependencies.'
    }

    $lines = @()
    foreach ($task in ($readyTasks | Select-Object -First $MaxCount)) {
        $lines += "$($task.id)`t$($task.priority)`t$($task.module)`t$($task.title)"
    }

    return ($lines -join [Environment]::NewLine)
}

function Write-TaskDetail {
    param(
        [object]$Catalog,
        [string]$Id
    )

    $tasks = Get-Tasks -Catalog $Catalog
    $task = Get-TaskById -Tasks $tasks -Id $Id
    if ($null -eq $task) {
        throw "Task not found: $Id"
    }

    $taskIndex = Get-TaskIndex -Tasks $tasks
    $lines = @()
    $lines += "Task: $($task.id)"
    $lines += "Title: $($task.title)"
    $lines += "Domain: $($task.domain)"
    $lines += "Module: $($task.module)"
    $lines += "Priority: $($task.priority)"
    $lines += "Status: $($task.status)"
    $lines += "Dependencies: $((Get-DependencySummary -Task $task -TaskIndex $taskIndex) -join ', ')"
    $lines += ''
    $lines += "Goal: $($task.goal)"
    $lines += "Scope: $($task.scope)"
    $lines += "Why now: $($task.whyNow)"
    $lines += ''
    $lines += 'Paths:'
    $lines += Format-BulletList -Values @($task.paths)
    $lines += ''
    $lines += 'Acceptance criteria:'
    $lines += Format-BulletList -Values @($task.acceptanceCriteria)
    $lines += ''
    $lines += 'Verify commands:'
    $lines += Format-BulletList -Values @($task.verifyCommands)

    return ($lines -join [Environment]::NewLine)
}

function Build-TaskPrompt {
    param(
        [object]$Catalog,
        [string]$Id
    )

    $tasks = Get-Tasks -Catalog $Catalog
    $task = Get-TaskById -Tasks $tasks -Id $Id
    if ($null -eq $task) {
        throw "Task not found: $Id"
    }

    $lines = @()
    $lines += "You are working in the HJO2OA repository on task $($task.id): $($task.title)."
    $lines += "Project stage: $($Catalog.metadata.stage)."
    $lines += ''
    $lines += "Goal: $($task.goal)"
    $lines += "Scope: $($task.scope)"
    $lines += "Why now: $($task.whyNow)"
    $lines += ''
    $lines += 'Global constraints:'
    $lines += Format-BulletList -Values @($Catalog.metadata.globalConstraints)
    $lines += ''
    $lines += 'Relevant paths and docs:'
    $lines += Format-BulletList -Values @($task.paths)
    $lines += ''
    $lines += 'Acceptance criteria:'
    $lines += Format-BulletList -Values @($task.acceptanceCriteria)
    $lines += ''
    $lines += 'Suggested verification commands:'
    $lines += Format-BulletList -Values @($task.verifyCommands)
    $lines += ''
    $lines += 'Definition of done:'
    $lines += '- Keep module boundaries clear and avoid writing into other business modules.'
    $lines += '- Prefer the smallest verifiable skeleton before adding real infrastructure integrations.'
    $lines += '- Finish with a concise change summary, verification result, and remaining risks.'

    return ($lines -join [Environment]::NewLine)
}

function Invoke-Verification {
    param([object]$Catalog)

    $repoRoot = Get-RepoRoot
    $catalogPath = Get-CatalogPath
    $analysisPath = Join-Path $repoRoot 'docs\engineering\automated-development-analysis.md'

    if (-not (Test-Path $catalogPath)) {
        throw "Missing task catalog: $catalogPath"
    }

    if (-not (Test-Path $analysisPath)) {
        throw "Missing analysis document: $analysisPath"
    }

    Push-Location $repoRoot
    try {
        Write-Output "Found task catalog: $catalogPath"
        Write-Output "Found analysis document: $analysisPath"
        Write-Output "Running verification: $($Catalog.metadata.verificationCommand)"

        & mvn -q test
        if ($LASTEXITCODE -ne 0) {
            throw "mvn -q test failed with exit code $LASTEXITCODE"
        }

        Write-Output 'Verification passed'
    } finally {
        Pop-Location
    }
}

$catalog = Get-TaskCatalog

switch ($Command) {
    'summary' {
        Write-Output (Write-Summary -Catalog $catalog)
    }
    'next' {
        Write-Output (Write-NextTasks -Catalog $catalog -MaxCount $Limit)
    }
    'show' {
        if ([string]::IsNullOrWhiteSpace($TaskId)) {
            throw 'show requires -TaskId'
        }
        Write-Output (Write-TaskDetail -Catalog $catalog -Id $TaskId)
    }
    'prompt' {
        if ([string]::IsNullOrWhiteSpace($TaskId)) {
            throw 'prompt requires -TaskId'
        }
        Write-Output (Build-TaskPrompt -Catalog $catalog -Id $TaskId)
    }
    'verify' {
        Invoke-Verification -Catalog $catalog
    }
    default {
        throw "Unsupported command: $Command"
    }
}
