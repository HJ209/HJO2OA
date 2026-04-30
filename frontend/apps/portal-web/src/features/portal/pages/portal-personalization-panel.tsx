import {
  useEffect,
  useMemo,
  useState,
  type FormEvent,
  type ReactElement,
} from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { RotateCcw, Save } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  fetchActivePortalPublication,
  fetchPersonalizationProfile,
  fetchPortalTemplateCanvas,
  resetPersonalizationProfile,
  savePersonalizationProfile,
} from '@/features/portal/services/portal-service'
import type {
  PersonalizationScope,
  PortalSceneType,
  PortalWidgetPlacementView,
  QuickAccessEntry,
  QuickAccessEntryType,
} from '@/features/portal/types/portal'
import {
  EmptyState,
  ErrorBanner,
  Field,
  LoadingBlock,
  PortalPanel,
  SelectInput,
  StatusBadge,
  SubmitButton,
  TextInput,
  ToggleField,
} from '@/features/portal/pages/portal-ui'
import { useIdentityStore } from '@/stores/identity-store'

const SCENE_OPTIONS: Array<{ value: PortalSceneType; label: string }> = [
  { value: 'HOME', label: 'HOME' },
  { value: 'OFFICE_CENTER', label: 'OFFICE_CENTER' },
  { value: 'MOBILE_WORKBENCH', label: 'MOBILE_WORKBENCH' },
]

const SCOPE_OPTIONS: Array<{ value: PersonalizationScope; label: string }> = [
  { value: 'ASSIGNMENT', label: '当前任职' },
  { value: 'GLOBAL', label: '用户全局' },
]

const QUICK_ACCESS_OPTIONS: Array<{
  value: QuickAccessEntryType
  label: string
}> = [
  { value: 'APP', label: 'APP' },
  { value: 'PROCESS', label: 'PROCESS' },
  { value: 'LINK', label: 'LINK' },
  { value: 'CONTENT', label: 'CONTENT' },
]

interface PlacementOption {
  placementCode: string
  widgetCode: string
  cardType: string
  pageCode: string
  regionCode: string
}

export default function PortalPersonalizationPanel(): ReactElement {
  const queryClient = useQueryClient()
  const currentAssignment = useIdentityStore((state) => state.currentAssignment)
  const [sceneType, setSceneType] = useState<PortalSceneType>('HOME')
  const [scope, setScope] = useState<PersonalizationScope>('ASSIGNMENT')
  const profileQuery = useQuery({
    queryKey: ['portal-personalization', 'profile', sceneType],
    queryFn: () => fetchPersonalizationProfile(sceneType),
  })
  const activePublicationQuery = useQuery({
    queryKey: ['portal-personalization', 'active-publication', sceneType],
    queryFn: () => fetchActivePortalPublication(sceneType, 'PC'),
    retry: false,
  })
  const canvasQuery = useQuery({
    queryKey: [
      'portal-personalization',
      'canvas',
      activePublicationQuery.data?.templateId,
    ],
    queryFn: () =>
      fetchPortalTemplateCanvas(activePublicationQuery.data?.templateId ?? ''),
    enabled: Boolean(activePublicationQuery.data?.templateId),
  })
  const placements = useMemo(
    () => extractPlacements(canvasQuery.data?.pages ?? []),
    [canvasQuery.data?.pages],
  )
  const [themeCode, setThemeCode] = useState('')
  const [orderedPlacementCodes, setOrderedPlacementCodes] = useState<string[]>(
    [],
  )
  const [hiddenPlacementCodes, setHiddenPlacementCodes] = useState<Set<string>>(
    () => new Set(),
  )
  const [quickAccessEntries, setQuickAccessEntries] = useState<
    QuickAccessEntry[]
  >([])

  useEffect(() => {
    const profile = profileQuery.data

    if (!profile) {
      return
    }

    setScope(profile.resolvedScope)
    setThemeCode(profile.themeCode ?? '')
    setHiddenPlacementCodes(new Set(profile.hiddenPlacementCodes))
    setQuickAccessEntries(profile.quickAccessEntries)
  }, [profileQuery.data])

  useEffect(() => {
    const profileOrder = profileQuery.data?.widgetOrderOverride ?? []
    const placementCodes = placements.map(
      (placement) => placement.placementCode,
    )
    const nextOrder = [
      ...profileOrder.filter((code) => placementCodes.includes(code)),
      ...placementCodes.filter((code) => !profileOrder.includes(code)),
    ]

    setOrderedPlacementCodes(nextOrder)
  }, [placements, profileQuery.data?.widgetOrderOverride])

  const invalidate = async (): Promise<void> => {
    await queryClient.invalidateQueries({
      queryKey: ['portal-personalization'],
    })
    await queryClient.invalidateQueries({ queryKey: ['portal-home'] })
  }
  const saveMutation = useMutation({
    mutationFn: () =>
      savePersonalizationProfile({
        sceneType,
        scope,
        assignmentId:
          scope === 'ASSIGNMENT' ? currentAssignment?.assignmentId : undefined,
        themeCode: themeCode || undefined,
        widgetOrderOverride: orderedPlacementCodes,
        hiddenPlacementCodes: [...hiddenPlacementCodes],
        quickAccessEntries: quickAccessEntries.map((entry, index) => ({
          ...entry,
          sortOrder: index + 1,
        })),
      }),
    onSuccess: invalidate,
  })
  const resetMutation = useMutation({
    mutationFn: () =>
      resetPersonalizationProfile({
        sceneType,
        scope,
        assignmentId:
          scope === 'ASSIGNMENT' ? currentAssignment?.assignmentId : undefined,
      }),
    onSuccess: invalidate,
  })

  function toggleHidden(placementCode: string, hidden: boolean): void {
    setHiddenPlacementCodes((current) => {
      const next = new Set(current)

      if (hidden) {
        next.add(placementCode)
      } else {
        next.delete(placementCode)
      }

      return next
    })
  }

  return (
    <div className="grid gap-4 2xl:grid-cols-[360px_minmax(0,1fr)]">
      <div className="space-y-4">
        <PortalPanel
          title="个性化上下文"
          description="读取当前身份命中的 personalization profile。"
        >
          <div className="space-y-3">
            <Field label="sceneType">
              <SelectInput
                options={SCENE_OPTIONS}
                value={sceneType}
                onChange={setSceneType}
              />
            </Field>
            <Field label="scope">
              <SelectInput
                options={SCOPE_OPTIONS}
                value={scope}
                onChange={setScope}
              />
            </Field>
            <Field label="themeCode">
              <TextInput value={themeCode} onChange={setThemeCode} />
            </Field>
            {profileQuery.data ? (
              <div className="space-y-2 rounded-xl bg-slate-50 p-3 text-sm">
                <InfoLine
                  label="profileId"
                  value={profileQuery.data.profileId}
                />
                <InfoLine
                  label="basePublicationId"
                  value={profileQuery.data.basePublicationId}
                />
                <InfoLine label="status" value={profileQuery.data.status} />
              </div>
            ) : null}
          </div>
        </PortalPanel>

        <PortalPanel
          title="快捷入口"
          description="快捷入口随 profile 保存，不在前端本地缓存。"
        >
          <QuickAccessEditor
            entries={quickAccessEntries}
            onChange={setQuickAccessEntries}
          />
        </PortalPanel>
      </div>

      <div className="space-y-4">
        <PortalPanel
          title="卡片排序与显隐"
          description="基于 active publication 的 template canvas 获取 placementCode。"
          actions={
            <div className="flex gap-2">
              <Button
                disabled={resetMutation.isPending}
                onClick={() => resetMutation.mutate()}
                size="sm"
                variant="outline"
              >
                <RotateCcw className="h-4 w-4" />
                恢复默认
              </Button>
            </div>
          }
        >
          {profileQuery.isLoading || canvasQuery.isLoading ? (
            <LoadingBlock />
          ) : null}
          {profileQuery.isError ? (
            <ErrorBanner error={profileQuery.error} title="个性化加载失败" />
          ) : null}
          {activePublicationQuery.isError ? (
            <ErrorBanner
              error={activePublicationQuery.error}
              title="生效发布解析失败"
            />
          ) : null}
          {canvasQuery.isError ? (
            <ErrorBanner error={canvasQuery.error} title="模板画布加载失败" />
          ) : null}
          {!placements.length && !canvasQuery.isLoading ? (
            <EmptyState
              title="没有可个性化的组件实例"
              description="当前场景没有生效发布模板，或模板画布没有组件实例。"
            />
          ) : null}
          {placements.length ? (
            <form
              className="space-y-4"
              onSubmit={(event: FormEvent<HTMLFormElement>) => {
                event.preventDefault()
                saveMutation.mutate()
              }}
            >
              <div className="space-y-2">
                {orderedPlacementCodes.map((placementCode, index) => {
                  const placement = placements.find(
                    (item) => item.placementCode === placementCode,
                  )

                  if (!placement) {
                    return null
                  }

                  return (
                    <div
                      className="grid gap-3 rounded-xl border border-slate-200 bg-white p-3 md:grid-cols-[1fr_auto]"
                      key={placement.placementCode}
                    >
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-medium text-slate-950">
                            {placement.widgetCode}
                          </span>
                          <StatusBadge>{placement.cardType}</StatusBadge>
                          <StatusBadge tone="sky">
                            {placement.regionCode}
                          </StatusBadge>
                        </div>
                        <p className="mt-1 text-xs text-slate-500">
                          {placement.pageCode} / {placement.placementCode}
                        </p>
                      </div>
                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          disabled={index === 0}
                          onClick={() =>
                            setOrderedPlacementCodes((current) =>
                              moveItem(current, index, index - 1),
                            )
                          }
                          size="sm"
                          variant="outline"
                        >
                          上移
                        </Button>
                        <Button
                          disabled={index === orderedPlacementCodes.length - 1}
                          onClick={() =>
                            setOrderedPlacementCodes((current) =>
                              moveItem(current, index, index + 1),
                            )
                          }
                          size="sm"
                          variant="outline"
                        >
                          下移
                        </Button>
                        <ToggleField
                          checked={hiddenPlacementCodes.has(
                            placement.placementCode,
                          )}
                          label="隐藏"
                          onChange={(hidden) =>
                            toggleHidden(placement.placementCode, hidden)
                          }
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
              <SubmitButton pending={saveMutation.isPending}>
                <Save className="h-4 w-4" />
                保存个性化
              </SubmitButton>
            </form>
          ) : null}
          {saveMutation.isError ? (
            <ErrorBanner error={saveMutation.error} title="个性化保存失败" />
          ) : null}
          {resetMutation.isError ? (
            <ErrorBanner error={resetMutation.error} title="恢复默认失败" />
          ) : null}
        </PortalPanel>
      </div>
    </div>
  )
}

function QuickAccessEditor({
  entries,
  onChange,
}: {
  entries: QuickAccessEntry[]
  onChange: (entries: QuickAccessEntry[]) => void
}): ReactElement {
  function updateEntry(index: number, patch: Partial<QuickAccessEntry>): void {
    onChange(
      entries.map((entry, entryIndex) =>
        entryIndex === index ? { ...entry, ...patch } : entry,
      ),
    )
  }

  return (
    <div className="space-y-3">
      {entries.map((entry, index) => (
        <div
          className="space-y-2 rounded-xl border border-slate-200 p-3"
          key={index}
        >
          <Field label="entryType">
            <SelectInput
              options={QUICK_ACCESS_OPTIONS}
              value={entry.entryType}
              onChange={(entryType) => updateEntry(index, { entryType })}
            />
          </Field>
          <Field label="targetCode">
            <TextInput
              value={entry.targetCode}
              onChange={(targetCode) => updateEntry(index, { targetCode })}
            />
          </Field>
          <Field label="targetLink">
            <TextInput
              value={entry.targetLink ?? ''}
              onChange={(targetLink) => updateEntry(index, { targetLink })}
            />
          </Field>
          <ToggleField
            checked={entry.pinned}
            label="置顶"
            onChange={(pinned) => updateEntry(index, { pinned })}
          />
          <div className="flex gap-2">
            <Button
              disabled={index === 0}
              onClick={() => onChange(moveItem(entries, index, index - 1))}
              size="sm"
              variant="outline"
            >
              上移
            </Button>
            <Button
              onClick={() =>
                onChange(
                  entries.filter((_, entryIndex) => entryIndex !== index),
                )
              }
              size="sm"
              variant="outline"
            >
              删除
            </Button>
          </div>
        </div>
      ))}
      <Button
        onClick={() =>
          onChange([
            ...entries,
            {
              entryType: 'APP',
              targetCode: `app-${entries.length + 1}`,
              targetLink: '/',
              icon: 'app',
              sortOrder: entries.length + 1,
              pinned: false,
            },
          ])
        }
        size="sm"
        variant="outline"
      >
        新增快捷入口
      </Button>
    </div>
  )
}

function InfoLine({
  label,
  value,
}: {
  label: string
  value?: string | null
}): ReactElement {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-slate-500">{label}</span>
      <span className="truncate font-medium text-slate-900">
        {value ?? '-'}
      </span>
    </div>
  )
}

function extractPlacements(
  pages: Array<{
    pageCode: string
    regions: Array<{
      regionCode: string
      placements: PortalWidgetPlacementView[]
    }>
  }>,
): PlacementOption[] {
  return pages.flatMap((page) =>
    page.regions.flatMap((region) =>
      region.placements.map((placement) => ({
        placementCode: placement.placementCode,
        widgetCode: placement.widgetCode,
        cardType: placement.cardType,
        pageCode: page.pageCode,
        regionCode: region.regionCode,
      })),
    ),
  )
}

function moveItem<T>(items: T[], fromIndex: number, toIndex: number): T[] {
  if (toIndex < 0 || toIndex >= items.length) {
    return items
  }

  const next = [...items]
  const [item] = next.splice(fromIndex, 1)

  next.splice(toIndex, 0, item)

  return next
}
