import type { ReactElement } from 'react'
import { ChevronDown, ChevronRight, FolderTree } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/utils/cn'
import type { OrgStructure } from '@/features/org-perm/types/org-perm'

export interface OrgTreeNodeProps {
  node: OrgStructure
  depth: number
  expanded: boolean
  selected: boolean
  onToggle: (id: string) => void
  onSelect: (node: OrgStructure) => void
  children?: ReactElement[]
}

export function OrgTreeNode({
  node,
  depth,
  expanded,
  selected,
  onToggle,
  onSelect,
  children,
}: OrgTreeNodeProps): ReactElement {
  const hasChildren = Boolean(node.children?.length)
  const ToggleIcon = expanded ? ChevronDown : ChevronRight

  return (
    <li>
      <div
        className={cn(
          'flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition',
          selected
            ? 'bg-sky-50 text-sky-700'
            : 'text-slate-700 hover:bg-slate-50',
        )}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
      >
        {hasChildren ? (
          <Button
            aria-label={expanded ? '折叠组织节点' : '展开组织节点'}
            className="h-6 w-6 rounded-lg"
            onClick={() => onToggle(node.id)}
            size="icon"
            variant="ghost"
          >
            <ToggleIcon className="h-4 w-4" />
          </Button>
        ) : (
          <span className="h-7 w-7" />
        )}
        <button
          className="flex min-w-0 flex-1 items-center gap-2 text-left"
          onClick={() => onSelect(node)}
          type="button"
        >
          <FolderTree className="h-4 w-4 shrink-0 text-slate-400" />
          <span className="truncate font-medium">{node.name}</span>
          <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-500">
            {node.code}
          </span>
        </button>
      </div>
      {expanded && children?.length ? <ul>{children}</ul> : null}
    </li>
  )
}
