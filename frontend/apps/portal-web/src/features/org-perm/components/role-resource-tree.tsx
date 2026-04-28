import type { ReactElement } from 'react'
import { Badge } from '@/components/ui/badge'
import type { ResourceNode } from '@/features/org-perm/types/org-perm'

export interface RoleResourceTreeProps {
  nodes: ResourceNode[]
  checkedIds: string[]
  onCheckedChange: (checkedIds: string[]) => void
}

function collectDescendantIds(node: ResourceNode): string[] {
  return [node.id, ...(node.children ?? []).flatMap(collectDescendantIds)]
}

function renderResourceNode(
  node: ResourceNode,
  depth: number,
  checkedSet: Set<string>,
  onCheckedChange: (node: ResourceNode, checked: boolean) => void,
): ReactElement {
  const checked = checkedSet.has(node.id)

  return (
    <li key={node.id}>
      <label
        className="flex items-center gap-3 rounded-lg px-2 py-2 text-sm hover:bg-slate-50"
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
      >
        <input
          aria-label={node.name}
          checked={checked}
          className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
          onChange={(event) => onCheckedChange(node, event.target.checked)}
          type="checkbox"
        />
        <span className="min-w-0 flex-1 truncate font-medium text-slate-800">
          {node.name}
        </span>
        <Badge variant={node.effect === 'ALLOW' ? 'success' : 'secondary'}>
          {node.type}
        </Badge>
      </label>
      {node.children?.length ? (
        <ul>
          {node.children.map((child) =>
            renderResourceNode(child, depth + 1, checkedSet, onCheckedChange),
          )}
        </ul>
      ) : null}
    </li>
  )
}

export function RoleResourceTree({
  nodes,
  checkedIds,
  onCheckedChange,
}: RoleResourceTreeProps): ReactElement {
  const checkedSet = new Set(checkedIds)

  function handleCheckedChange(node: ResourceNode, checked: boolean): void {
    const next = new Set(checkedSet)
    const affectedIds = collectDescendantIds(node)

    affectedIds.forEach((id) => {
      if (checked) {
        next.add(id)
      } else {
        next.delete(id)
      }
    })

    onCheckedChange(Array.from(next))
  }

  if (nodes.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-200 p-6 text-center text-sm text-slate-500">
        暂无资源权限
      </div>
    )
  }

  return (
    <ul aria-label="资源权限树" className="space-y-1">
      {nodes.map((node) =>
        renderResourceNode(node, 0, checkedSet, handleCheckedChange),
      )}
    </ul>
  )
}
