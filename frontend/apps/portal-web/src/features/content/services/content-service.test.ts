import { afterEach, describe, expect, it, vi } from 'vitest'
import { get, post } from '@/services/request'
import {
  getArticles,
  getContentCategoryTree,
  publishArticle,
  rollbackArticle,
} from '@/features/content/services/content-service'

vi.mock('@/services/request', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)

afterEach(() => {
  vi.clearAllMocks()
})

describe('content-service', () => {
  it('loads category tree with enabledOnly query', async () => {
    mockedGet.mockResolvedValueOnce([])

    await getContentCategoryTree(true)

    expect(mockedGet).toHaveBeenCalledWith(
      '/v1/content/categories/tree?enabledOnly=true',
    )
  })

  it('serializes article list filters', async () => {
    mockedGet.mockResolvedValueOnce({ items: [], page: 1, size: 20, total: 0 })

    await getArticles({ status: 'PUBLISHED', keyword: 'policy', page: 2 })

    expect(mockedGet).toHaveBeenCalledWith(
      expect.stringContaining('/v1/content/articles?'),
    )
    expect(mockedGet).toHaveBeenCalledWith(
      expect.stringContaining('status=PUBLISHED'),
    )
    expect(mockedGet).toHaveBeenCalledWith(
      expect.stringContaining('keyword=policy'),
    )
  })

  it('publishes articles with idempotency metadata', async () => {
    mockedPost.mockResolvedValueOnce({})

    await publishArticle('article-1', {
      operatorId: 'operator-1',
      reviewMode: 'DIRECT',
      scopes: [{ subjectType: 'ALL', effect: 'ALLOW' }],
    })

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/content/articles/article-1/publish',
      expect.objectContaining({ reviewMode: 'DIRECT' }),
      expect.objectContaining({
        dedupeKey: 'content-article:publish:article-1:draft',
        idempotencyKey: 'content-article:publish:article-1:draft',
      }),
    )
  })

  it('rolls back to target version through article lifecycle endpoint', async () => {
    mockedPost.mockResolvedValueOnce({})

    await rollbackArticle('article-1', 'operator-1', 2)

    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/content/articles/article-1/rollback',
      { operatorId: 'operator-1', targetVersionNo: 2 },
      expect.objectContaining({
        idempotencyKey: 'content-article:rollback:article-1:2',
      }),
    )
  })
})
