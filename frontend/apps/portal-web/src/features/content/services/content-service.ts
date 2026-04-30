import { get, post, put } from '@/services/request'
import type {
  ArticleDetail,
  ArticleListQuery,
  ArticlePage,
  ContentCategoryTreeNode,
  ContentPublication,
  ContentVersion,
  PublishArticleInput,
  UpsertArticleInput,
  UpsertCategoryInput,
} from '@/features/content/types/content'

const CONTENT_PREFIX = '/v1/content'

function toQueryString(query: Record<string, unknown>): string {
  const params = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === '') {
      continue
    }
    params.set(key, String(value))
  }

  const serialized = params.toString()

  return serialized ? `?${serialized}` : ''
}

export async function getContentCategoryTree(
  enabledOnly = false,
): Promise<ContentCategoryTreeNode[]> {
  return get<ContentCategoryTreeNode[]>(
    `${CONTENT_PREFIX}/categories/tree${toQueryString({ enabledOnly })}`,
  )
}

export async function createContentCategory(
  input: UpsertCategoryInput,
): Promise<ContentCategoryTreeNode['category']> {
  return post<ContentCategoryTreeNode['category'], UpsertCategoryInput>(
    `${CONTENT_PREFIX}/categories`,
    input,
    {
      dedupeKey: `content-category:create:${input.code}`,
      idempotencyKey: `content-category:create:${input.code}`,
    },
  )
}

export async function updateContentCategory(
  categoryId: string,
  input: UpsertCategoryInput,
): Promise<ContentCategoryTreeNode['category']> {
  return put<ContentCategoryTreeNode['category'], UpsertCategoryInput>(
    `${CONTENT_PREFIX}/categories/${categoryId}`,
    input,
    {
      dedupeKey: `content-category:update:${categoryId}`,
      idempotencyKey: `content-category:update:${categoryId}:${input.code}`,
    },
  )
}

export async function setContentCategoryEnabled(
  categoryId: string,
  operatorId: string,
  enabled: boolean,
): Promise<ContentCategoryTreeNode['category']> {
  return post<ContentCategoryTreeNode['category'], { operatorId: string }>(
    `${CONTENT_PREFIX}/categories/${categoryId}/${enabled ? 'enable' : 'disable'}`,
    { operatorId },
    {
      dedupeKey: `content-category:${enabled ? 'enable' : 'disable'}:${categoryId}`,
      idempotencyKey: `content-category:${enabled ? 'enable' : 'disable'}:${categoryId}`,
    },
  )
}

export async function getArticles(
  query: ArticleListQuery = {},
): Promise<ArticlePage> {
  return get<ArticlePage>(
    `${CONTENT_PREFIX}/articles${toQueryString({
      categoryId: query.categoryId,
      status: query.status,
      authorId: query.authorId,
      keyword: query.keyword,
      page: query.page ?? 1,
      size: query.size ?? 20,
    })}`,
  )
}

export async function getArticle(articleId: string): Promise<ArticleDetail> {
  return get<ArticleDetail>(`${CONTENT_PREFIX}/articles/${articleId}`)
}

export async function createArticle(
  input: UpsertArticleInput,
): Promise<ArticleDetail> {
  return post<ArticleDetail, UpsertArticleInput>(
    `${CONTENT_PREFIX}/articles`,
    input,
    {
      dedupeKey: `content-article:create:${input.articleNo ?? input.title}`,
    },
  )
}

export async function updateArticle(
  articleId: string,
  input: UpsertArticleInput,
): Promise<ArticleDetail> {
  return put<ArticleDetail, UpsertArticleInput>(
    `${CONTENT_PREFIX}/articles/${articleId}`,
    input,
    {
      dedupeKey: `content-article:update:${articleId}`,
    },
  )
}

export async function publishArticle(
  articleId: string,
  input: PublishArticleInput,
): Promise<ArticleDetail> {
  return post<ArticleDetail, PublishArticleInput>(
    `${CONTENT_PREFIX}/articles/${articleId}/publish`,
    input,
    {
      dedupeKey: `content-article:publish:${articleId}:${input.versionNo ?? 'draft'}`,
      idempotencyKey: `content-article:publish:${articleId}:${input.versionNo ?? 'draft'}`,
    },
  )
}

export async function unpublishArticle(
  articleId: string,
  operatorId: string,
  reason: string,
): Promise<ArticleDetail> {
  return post<ArticleDetail, { operatorId: string; reason: string }>(
    `${CONTENT_PREFIX}/articles/${articleId}/unpublish`,
    { operatorId, reason },
    {
      dedupeKey: `content-article:unpublish:${articleId}`,
      idempotencyKey: `content-article:unpublish:${articleId}`,
    },
  )
}

export async function submitArticleReview(
  articleId: string,
  operatorId: string,
): Promise<ArticleDetail> {
  return post<ArticleDetail, { operatorId: string; opinion: string }>(
    `${CONTENT_PREFIX}/articles/${articleId}/submit`,
    { operatorId, opinion: 'Submitted from content workspace' },
    { dedupeKey: `content-article:submit:${articleId}` },
  )
}

export async function approveArticle(
  articleId: string,
  operatorId: string,
): Promise<ArticleDetail> {
  return post<ArticleDetail, { operatorId: string; opinion: string }>(
    `${CONTENT_PREFIX}/articles/${articleId}/approve`,
    { operatorId, opinion: 'Approved from content workspace' },
    { dedupeKey: `content-article:approve:${articleId}` },
  )
}

export async function rejectArticle(
  articleId: string,
  operatorId: string,
): Promise<ArticleDetail> {
  return post<ArticleDetail, { operatorId: string; opinion: string }>(
    `${CONTENT_PREFIX}/articles/${articleId}/reject`,
    { operatorId, opinion: 'Rejected from content workspace' },
    { dedupeKey: `content-article:reject:${articleId}` },
  )
}

export async function rollbackArticle(
  articleId: string,
  operatorId: string,
  targetVersionNo: number,
): Promise<ArticleDetail> {
  return post<ArticleDetail, { operatorId: string; targetVersionNo: number }>(
    `${CONTENT_PREFIX}/articles/${articleId}/rollback`,
    { operatorId, targetVersionNo },
    {
      dedupeKey: `content-article:rollback:${articleId}:${targetVersionNo}`,
      idempotencyKey: `content-article:rollback:${articleId}:${targetVersionNo}`,
    },
  )
}

export async function getArticleHistory(
  articleId: string,
): Promise<ContentVersion[]> {
  return get<ContentVersion[]>(
    `${CONTENT_PREFIX}/articles/${articleId}/history`,
  )
}

export async function getArticlePublications(
  articleId: string,
): Promise<ContentPublication[]> {
  return get<ContentPublication[]>(
    `${CONTENT_PREFIX}/articles/${articleId}/publications`,
  )
}
