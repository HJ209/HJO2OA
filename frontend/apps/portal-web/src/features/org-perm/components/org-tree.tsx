import { useMemo, useState, type ReactElement } from 'react'
import { OrgTreeNode } from '@/features/org-perm/components/org-tree-node'
import type { OrgStructure } from '@/features/org-perm/types/org-perm'

export interface OrgTreeProps {
  nodes: OrgStructure[]
  selectedId?: string
  onSelect: (node: OrgStructure) => void
}

function collectDefaultExpanded(nodes: OrgStructure[]): string[] {
  return nodes
    .filter((node) => node.children?.length)
    .map((node) => node.id)
    .slice(0, 3)
}

export function OrgTree({
  nodes,
  selectedId,
  onSelect,
}: OrgTreeProps): ReactElement {
  const defaultExpanded = useMemo(() => collectDefaultExpanded(nodes), [nodes])
  const [expandedIds, setExpandedIds] = useState<Set<string>>(
    () => new Set(defaultExpanded),
  )

  function handleToggle(id: string): void {
    setExpandedIds((current) => {
      const next = new Set(current)

      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
      }

      return next
    })
  }

  function renderNode(node: OrgStructure, depth: number): ReactElement {
    return (
      <OrgTreeNode
        depth={depth}
        expanded={expandedIds.has(node.id)}
        key={node.id}
        node={node}
        onSelect={onSelect}
        onToggle={handleToggle}
        selected={selectedId === node.id}
      >
        {node.children?.map((child) => renderNode(child, depth + 1))}
      </OrgTreeNode>
    )
  }

  if (nodes.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无组织数据
      </div>
    )
  }

  return (
    <ul aria-label="组织树" className="space-y-1">
      {nodes.map((node) => renderNode(node, 0))}
    </ul>
  )
}
