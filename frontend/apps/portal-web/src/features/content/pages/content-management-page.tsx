import { useEffect, useMemo, useState, type ReactElement } from 'react'
import {
  QueryClient,
  QueryClientProvider,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query'
import {
  CheckCircle2,
  Eye,
  FilePenLine,
  FolderTree,
  History,
  Power,
  RotateCcw,
  Save,
  Search,
  Send,
  UploadCloud,
  XCircle,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useAuthStore } from '@/stores/auth-store'
import { cn } from '@/utils/cn'
import {
  approveArticle,
  createArticle,
  createContentCategory,
  getArticle,
  getArticleHistory,
  getArticles,
  getContentCategoryTree,
  publishArticle,
  rejectArticle,
  rollbackArticle,
  setContentCategoryEnabled,
  submitArticleReview,
  unpublishArticle,
  updateArticle,
} from '@/features/content/services/content-service'
import type {
  ArticleListQuery,
  ArticleStatus,
  ContentCategory,
  ContentCategoryTreeNode,
  ContentVersion,
  PublicationScopeRuleInput,
  UpsertArticleInput,
  UpsertCategoryInput,
} from '@/features/content/types/content'

type PanelKey = 'articles' | 'editor' | 'preview' | 'versions'

const OPERATOR_FALLBACK = '00000000-0000-0000-0000-000000000101'

const STATUS_OPTIONS: Array<{ label: string; value: ArticleStatus | '' }> = [
  { label: 'All', value: '' },
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Reviewing', value: 'REVIEWING' },
  { label: 'Published', value: 'PUBLISHED' },
  { label: 'Offline', value: 'OFFLINE' },
  { label: 'Archived', value: 'ARCHIVED' },
]

function isUuid(value: string | null | undefined): value is string {
  return Boolean(
    value &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    ),
  )
}

function flattenTree(
  nodes: ContentCategoryTreeNode[],
  level = 0,
): Array<{ category: ContentCategory; level: number }> {
  return nodes.flatMap((node) => [
    { category: node.category, level },
    ...flattenTree(node.children, level + 1),
  ])
}

function createEmptyArticleDraft(
  operatorId: string,
  categoryId?: string,
): UpsertArticleInput {
  return {
    operatorId,
    articleNo: `ART-${Date.now()}`,
    title: '',
    summary: '',
    bodyFormat: 'MARKDOWN',
    bodyText: '',
    coverAttachmentId: null,
    attachments: [],
    tags: [],
    contentType: 'ARTICLE',
    mainCategoryId: categoryId ?? '',
    authorId: operatorId,
    authorName: 'Content Editor',
    sourceType: 'ORIGINAL',
    sourceUrl: null,
  }
}

function createEmptyCategoryDraft(operatorId: string): UpsertCategoryInput {
  return {
    operatorId,
    code: '',
    name: '',
    categoryType: 'GENERAL',
    parentId: null,
    routePath: '',
    sortOrder: 0,
    visibleMode: 'INHERIT',
    permissions: [{ subjectType: 'ALL', effect: 'ALLOW', scope: 'MANAGE' }],
  }
}

function toTags(value: string): string[] {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function ContentManagementContent(): ReactElement {
  const queryClient = useQueryClient()
  const authUser = useAuthStore((state) => state.user)
  const operatorId = isUuid(authUser?.id) ? authUser.id : OPERATOR_FALLBACK
  const [panel, setPanel] = useState<PanelKey>('articles')
  const [selectedCategoryId, setSelectedCategoryId] = useState<string>()
  const [selectedArticleId, setSelectedArticleId] = useState<string>()
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState<ArticleStatus | ''>('')
  const [categoryDraft, setCategoryDraft] = useState(() =>
    createEmptyCategoryDraft(operatorId),
  )
  const [articleDraft, setArticleDraft] = useState(() =>
    createEmptyArticleDraft(operatorId),
  )
  const [tagText, setTagText] = useState('')

  const categoriesQuery = useQuery({
    queryKey: ['content', 'categories'],
    queryFn: () => getContentCategoryTree(false),
  })
  const flatCategories = useMemo(
    () => flattenTree(categoriesQuery.data ?? []),
    [categoriesQuery.data],
  )
  const articlesQuery = useQuery({
    queryKey: ['content', 'articles', selectedCategoryId, status, keyword],
    queryFn: () =>
      getArticles({
        categoryId: selectedCategoryId,
        status: status || undefined,
        keyword,
        page: 1,
        size: 30,
      } satisfies ArticleListQuery),
  })
  const selectedArticleQuery = useQuery({
    queryKey: ['content', 'article', selectedArticleId],
    queryFn: () => getArticle(selectedArticleId ?? ''),
    enabled: Boolean(selectedArticleId),
  })
  const historyQuery = useQuery({
    queryKey: ['content', 'article-history', selectedArticleId],
    queryFn: () => getArticleHistory(selectedArticleId ?? ''),
    enabled: Boolean(selectedArticleId),
  })

  useEffect(() => {
    if (flatCategories.length > 0 && !selectedCategoryId) {
      const firstEnabled = flatCategories.find(
        (item) => item.category.status === 'ENABLED',
      )
      setSelectedCategoryId(
        firstEnabled?.category.id ?? flatCategories[0].category.id,
      )
    }
  }, [flatCategories, selectedCategoryId])

  useEffect(() => {
    if (selectedArticleQuery.data) {
      const detail = selectedArticleQuery.data
      setArticleDraft({
        operatorId,
        articleNo: detail.article.articleNo,
        title: detail.currentVersion.title,
        summary: detail.currentVersion.summary ?? detail.article.summary ?? '',
        bodyFormat: detail.currentVersion.bodyFormat,
        bodyText: detail.currentVersion.bodyText,
        coverAttachmentId: detail.currentVersion.coverAttachmentId,
        attachments: detail.currentVersion.attachments,
        tags: detail.currentVersion.tags,
        contentType: detail.article.contentType,
        mainCategoryId: detail.article.mainCategoryId,
        authorId: detail.article.authorId,
        authorName: detail.article.authorName,
        sourceType: 'ORIGINAL',
        sourceUrl: null,
      })
      setTagText(detail.currentVersion.tags.join(', '))
    }
  }, [operatorId, selectedArticleQuery.data])

  const refreshContent = async (): Promise<void> => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['content', 'categories'] }),
      queryClient.invalidateQueries({ queryKey: ['content', 'articles'] }),
      queryClient.invalidateQueries({ queryKey: ['content', 'article'] }),
      queryClient.invalidateQueries({
        queryKey: ['content', 'article-history'],
      }),
    ])
  }

  const categoryMutation = useMutation({
    mutationFn: createContentCategory,
    onSuccess: async () => {
      setCategoryDraft(createEmptyCategoryDraft(operatorId))
      await refreshContent()
    },
  })
  const categoryToggleMutation = useMutation({
    mutationFn: ({
      categoryId,
      enabled,
    }: {
      categoryId: string
      enabled: boolean
    }) => setContentCategoryEnabled(categoryId, operatorId, enabled),
    onSuccess: refreshContent,
  })
  const saveArticleMutation = useMutation({
    mutationFn: async (input: UpsertArticleInput) => {
      if (selectedArticleId) {
        return updateArticle(selectedArticleId, input)
      }

      return createArticle(input)
    },
    onSuccess: async (detail) => {
      setSelectedArticleId(detail.article.id)
      setPanel('preview')
      await refreshContent()
    },
  })
  const publishMutation = useMutation({
    mutationFn: (reviewMode: 'DIRECT' | 'REVIEW') => {
      if (!selectedArticleId) {
        throw new Error('No article selected')
      }
      const scopes: PublicationScopeRuleInput[] = [
        { subjectType: 'ALL', effect: 'ALLOW' },
      ]

      return publishArticle(selectedArticleId, {
        operatorId,
        reviewMode,
        scopes,
        reason: 'Published from content workspace',
      })
    },
    onSuccess: refreshContent,
  })
  const simpleActionMutation = useMutation({
    mutationFn: async (
      action: 'submit' | 'approve' | 'reject' | 'unpublish',
    ) => {
      if (!selectedArticleId) {
        throw new Error('No article selected')
      }
      if (action === 'submit') {
        return submitArticleReview(selectedArticleId, operatorId)
      }
      if (action === 'approve') {
        return approveArticle(selectedArticleId, operatorId)
      }
      if (action === 'reject') {
        return rejectArticle(selectedArticleId, operatorId)
      }

      return unpublishArticle(
        selectedArticleId,
        operatorId,
        'Offline from content workspace',
      )
    },
    onSuccess: refreshContent,
  })
  const rollbackMutation = useMutation({
    mutationFn: (versionNo: number) => {
      if (!selectedArticleId) {
        throw new Error('No article selected')
      }

      return rollbackArticle(selectedArticleId, operatorId, versionNo)
    },
    onSuccess: refreshContent,
  })

  const selectedArticle = selectedArticleQuery.data
  const isBusy =
    saveArticleMutation.isPending ||
    publishMutation.isPending ||
    simpleActionMutation.isPending ||
    rollbackMutation.isPending

  const updateArticleDraft = <TKey extends keyof UpsertArticleInput>(
    key: TKey,
    value: UpsertArticleInput[TKey],
  ): void => {
    setArticleDraft((current) => ({ ...current, [key]: value }))
  }

  const saveArticle = (): void => {
    saveArticleMutation.mutate({
      ...articleDraft,
      operatorId,
      mainCategoryId: articleDraft.mainCategoryId || selectedCategoryId || '',
      tags: toTags(tagText),
    })
  }

  const startNewArticle = (): void => {
    setSelectedArticleId(undefined)
    setArticleDraft(createEmptyArticleDraft(operatorId, selectedCategoryId))
    setTagText('')
    setPanel('editor')
  }

  return (
    <div className="space-y-5">
      <section className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 pb-4">
        <div>
          <Badge>Content</Badge>
          <h1 className="mt-2 text-2xl font-semibold text-slate-950">
            Content management
          </h1>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button onClick={startNewArticle}>
            <FilePenLine className="h-4 w-4" />
            New article
          </Button>
          <Button
            disabled={!selectedArticleId || isBusy}
            onClick={() => publishMutation.mutate('DIRECT')}
            variant="outline"
          >
            <UploadCloud className="h-4 w-4" />
            Publish
          </Button>
          <Button
            disabled={!selectedArticleId || isBusy}
            onClick={() => simpleActionMutation.mutate('unpublish')}
            variant="outline"
          >
            <Power className="h-4 w-4" />
            Offline
          </Button>
        </div>
      </section>

      <div className="grid gap-5 xl:grid-cols-[280px_minmax(0,1fr)]">
        <aside className="space-y-4 rounded-xl border border-slate-200 bg-white p-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
            <FolderTree className="h-4 w-4 text-sky-600" />
            Categories
          </div>
          {categoriesQuery.isLoading ? (
            <Skeleton className="h-28 w-full" />
          ) : null}
          <div className="space-y-1">
            {flatCategories.map(({ category, level }) => (
              <button
                className={cn(
                  'flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm transition',
                  selectedCategoryId === category.id
                    ? 'bg-sky-50 text-sky-800'
                    : 'text-slate-600 hover:bg-slate-50',
                )}
                key={category.id}
                onClick={() => setSelectedCategoryId(category.id)}
                style={{ paddingLeft: 12 + level * 16 }}
                type="button"
              >
                <span>{category.name}</span>
                <span className="text-xs text-slate-400">
                  {category.status}
                </span>
              </button>
            ))}
          </div>
          <div className="space-y-2 border-t border-slate-100 pt-3">
            <Input
              onChange={(event) =>
                setCategoryDraft((current) => ({
                  ...current,
                  code: event.target.value,
                  routePath: `/${event.target.value}`,
                }))
              }
              placeholder="category code"
              value={categoryDraft.code}
            />
            <Input
              onChange={(event) =>
                setCategoryDraft((current) => ({
                  ...current,
                  name: event.target.value,
                }))
              }
              placeholder="category name"
              value={categoryDraft.name}
            />
            <Button
              disabled={!categoryDraft.code || !categoryDraft.name}
              onClick={() => categoryMutation.mutate(categoryDraft)}
              size="sm"
              variant="outline"
            >
              <Save className="h-4 w-4" />
              Save category
            </Button>
            {selectedCategoryId ? (
              <div className="flex gap-2">
                <Button
                  onClick={() =>
                    categoryToggleMutation.mutate({
                      categoryId: selectedCategoryId,
                      enabled: true,
                    })
                  }
                  size="sm"
                  variant="ghost"
                >
                  <CheckCircle2 className="h-4 w-4" />
                </Button>
                <Button
                  onClick={() =>
                    categoryToggleMutation.mutate({
                      categoryId: selectedCategoryId,
                      enabled: false,
                    })
                  }
                  size="sm"
                  variant="ghost"
                >
                  <XCircle className="h-4 w-4" />
                </Button>
              </div>
            ) : null}
          </div>
        </aside>

        <main className="space-y-4">
          <div className="flex flex-wrap gap-2 rounded-xl border border-slate-200 bg-white p-2">
            {(
              [
                ['articles', Search],
                ['editor', FilePenLine],
                ['preview', Eye],
                ['versions', History],
              ] as const
            ).map(([key, Icon]) => (
              <button
                className={cn(
                  'flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium',
                  panel === key
                    ? 'bg-slate-900 text-white'
                    : 'text-slate-600 hover:bg-slate-50',
                )}
                key={key}
                onClick={() => setPanel(key)}
                type="button"
              >
                <Icon className="h-4 w-4" />
                {key}
              </button>
            ))}
          </div>

          {panel === 'articles' ? (
            <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
              <div className="grid gap-3 md:grid-cols-[1fr_180px]">
                <Input
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="Search title, summary, body"
                  value={keyword}
                />
                <select
                  className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
                  onChange={(event) =>
                    setStatus(event.target.value as ArticleStatus | '')
                  }
                  value={status}
                >
                  {STATUS_OPTIONS.map((option) => (
                    <option key={option.label} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              {articlesQuery.isLoading ? (
                <Skeleton className="h-40 w-full" />
              ) : null}
              <div className="divide-y divide-slate-100">
                {(articlesQuery.data?.items ?? []).map((article) => (
                  <button
                    className={cn(
                      'grid w-full gap-2 px-2 py-3 text-left hover:bg-slate-50 md:grid-cols-[1fr_130px_120px]',
                      selectedArticleId === article.id && 'bg-sky-50',
                    )}
                    key={article.id}
                    onClick={() => {
                      setSelectedArticleId(article.id)
                      setPanel('preview')
                    }}
                    type="button"
                  >
                    <span>
                      <span className="block font-medium text-slate-950">
                        {article.title}
                      </span>
                      <span className="line-clamp-1 text-sm text-slate-500">
                        {article.summary}
                      </span>
                    </span>
                    <span className="text-sm text-slate-500">
                      {article.authorName ?? article.authorId}
                    </span>
                    <Badge
                      variant={
                        article.status === 'PUBLISHED' ? 'success' : 'secondary'
                      }
                    >
                      {article.status}
                    </Badge>
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          {panel === 'editor' ? (
            <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
              <div className="grid gap-3 md:grid-cols-[180px_1fr]">
                <Input
                  onChange={(event) =>
                    updateArticleDraft('articleNo', event.target.value)
                  }
                  placeholder="Article no"
                  value={articleDraft.articleNo ?? ''}
                />
                <Input
                  onChange={(event) =>
                    updateArticleDraft('title', event.target.value)
                  }
                  placeholder="Title"
                  value={articleDraft.title}
                />
              </div>
              <Input
                onChange={(event) =>
                  updateArticleDraft('summary', event.target.value)
                }
                placeholder="Summary"
                value={articleDraft.summary ?? ''}
              />
              <div className="grid gap-3 md:grid-cols-[1fr_1fr]">
                <select
                  className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm"
                  onChange={(event) =>
                    updateArticleDraft('mainCategoryId', event.target.value)
                  }
                  value={
                    articleDraft.mainCategoryId || selectedCategoryId || ''
                  }
                >
                  <option value="">Select category</option>
                  {flatCategories.map(({ category, level }) => (
                    <option key={category.id} value={category.id}>
                      {'-'.repeat(level)} {category.name}
                    </option>
                  ))}
                </select>
                <Input
                  onChange={(event) => setTagText(event.target.value)}
                  placeholder="Tags separated by comma"
                  value={tagText}
                />
              </div>
              <textarea
                className="min-h-80 w-full resize-y rounded-xl border border-slate-200 p-3 text-sm outline-none focus:border-sky-400 focus:ring-2 focus:ring-sky-100"
                onChange={(event) =>
                  updateArticleDraft('bodyText', event.target.value)
                }
                placeholder="Body"
                value={articleDraft.bodyText}
              />
              <div className="flex flex-wrap gap-2">
                <Button disabled={isBusy} onClick={saveArticle}>
                  <Save className="h-4 w-4" />
                  Save draft
                </Button>
                <Button
                  disabled={!selectedArticleId || isBusy}
                  onClick={() => simpleActionMutation.mutate('submit')}
                  variant="outline"
                >
                  <Send className="h-4 w-4" />
                  Submit
                </Button>
                <Button
                  disabled={!selectedArticleId || isBusy}
                  onClick={() => simpleActionMutation.mutate('approve')}
                  variant="outline"
                >
                  <CheckCircle2 className="h-4 w-4" />
                  Approve
                </Button>
                <Button
                  disabled={!selectedArticleId || isBusy}
                  onClick={() => simpleActionMutation.mutate('reject')}
                  variant="outline"
                >
                  <XCircle className="h-4 w-4" />
                  Reject
                </Button>
                <Button
                  disabled={!selectedArticleId || isBusy}
                  onClick={() => publishMutation.mutate('REVIEW')}
                  variant="outline"
                >
                  <UploadCloud className="h-4 w-4" />
                  Publish reviewed
                </Button>
              </div>
            </section>
          ) : null}

          {panel === 'preview' ? (
            <section className="rounded-xl border border-slate-200 bg-white p-5">
              {selectedArticleQuery.isLoading ? (
                <Skeleton className="h-60 w-full" />
              ) : null}
              {selectedArticle ? (
                <article className="prose max-w-none">
                  <div className="mb-4 flex flex-wrap items-center gap-2">
                    <Badge
                      variant={
                        selectedArticle.article.status === 'PUBLISHED'
                          ? 'success'
                          : 'secondary'
                      }
                    >
                      {selectedArticle.article.status}
                    </Badge>
                    <span className="text-sm text-slate-500">
                      v{selectedArticle.currentVersion.versionNo}
                    </span>
                  </div>
                  <h2 className="text-2xl font-semibold text-slate-950">
                    {selectedArticle.currentVersion.title}
                  </h2>
                  <p className="text-slate-500">
                    {selectedArticle.currentVersion.summary}
                  </p>
                  <pre className="mt-5 whitespace-pre-wrap rounded-xl bg-slate-50 p-4 text-sm text-slate-800">
                    {selectedArticle.currentVersion.bodyText}
                  </pre>
                </article>
              ) : (
                <div className="py-12 text-center text-sm text-slate-500">
                  Select or create an article.
                </div>
              )}
            </section>
          ) : null}

          {panel === 'versions' ? (
            <section className="space-y-3 rounded-xl border border-slate-200 bg-white p-4">
              {(historyQuery.data ?? []).map((version: ContentVersion) => (
                <div
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-100 p-3"
                  key={version.id}
                >
                  <div>
                    <div className="font-medium text-slate-950">
                      v{version.versionNo} {version.title}
                    </div>
                    <div className="text-sm text-slate-500">
                      {version.status} · {version.updatedAt}
                    </div>
                  </div>
                  <Button
                    disabled={isBusy}
                    onClick={() => rollbackMutation.mutate(version.versionNo)}
                    size="sm"
                    variant="outline"
                  >
                    <RotateCcw className="h-4 w-4" />
                    Rollback
                  </Button>
                </div>
              ))}
              {!selectedArticleId ? (
                <div className="py-8 text-center text-sm text-slate-500">
                  Select an article to view versions.
                </div>
              ) : null}
            </section>
          ) : null}
        </main>
      </div>
    </div>
  )
}

export default function ContentManagementPage(): ReactElement {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            retry: 1,
            refetchOnWindowFocus: false,
            staleTime: 15000,
          },
        },
      }),
  )

  return (
    <QueryClientProvider client={queryClient}>
      <ContentManagementContent />
    </QueryClientProvider>
  )
}
