import { beforeEach, describe, expect, it, vi } from 'vitest'
import { dataI18nService } from '@/features/infra-admin/services/data-i18n-service'
import { errorCodeService } from '@/features/infra-admin/services/error-code-service'
import { i18nService } from '@/features/infra-admin/services/i18n-service'
import { timezoneService } from '@/features/infra-admin/services/timezone-service'
import { BizError } from '@/services/error-mapper'
import { get, post, put } from '@/services/request'

vi.mock('@/services/request', () => ({
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedPost = vi.mocked(post)
const mockedPut = vi.mocked(put)

describe('window 06 infra i18n services', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('lists i18n bundles through the bundle endpoint and pages client-side', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'bundle-1',
        bundleCode: 'error.messages',
        moduleCode: 'infra',
        locale: 'en-US',
        fallbackLocale: null,
        status: 'ACTIVE',
        tenantId: null,
        entries: [
          {
            id: 'entry-1',
            localeBundleId: 'bundle-1',
            resourceKey: 'shared.bad_request',
            resourceValue: 'Invalid request',
            version: 1,
            active: true,
          },
        ],
      },
    ])

    const page = await i18nService.list({
      page: 1,
      size: 20,
      moduleCode: 'infra',
      locale: 'en-US',
      keyword: 'bad_request',
    })

    const params = mockedGet.mock.calls[0][1]?.params as URLSearchParams
    expect(mockedGet.mock.calls[0][0]).toBe('/v1/infra/i18n/bundles')
    expect(params.get('moduleCode')).toBe('infra')
    expect(params.get('locale')).toBe('en-US')
    expect(page.items[0]).toEqual(
      expect.objectContaining({
        id: 'bundle-1',
        bundleCode: 'error.messages',
        entries: [
          expect.objectContaining({ resourceKey: 'shared.bad_request' }),
        ],
      }),
    )
  })

  it('uses tenant-aware data-i18n endpoints for list, batch and resolve', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'tr-1',
        entityType: 'article',
        entityId: 'A-1',
        fieldName: 'title',
        locale: 'zh-CN',
        translatedValue: 'Title CN',
        translationStatus: 'TRANSLATED',
        tenantId: 'tenant-1',
      },
    ])
    mockedPost.mockResolvedValueOnce([]).mockResolvedValueOnce({
      entityType: 'article',
      entityId: 'A-1',
      fieldName: 'title',
      requestedLocale: 'en-US',
      resolvedLocale: 'zh-CN',
      resolvedValue: 'Title CN',
      resolveSource: 'FALLBACK',
      fallbackApplied: true,
      tenantId: 'tenant-1',
    })

    await dataI18nService.list({
      page: 1,
      size: 20,
      tenantId: 'tenant-1',
      entityType: 'article',
      locale: 'zh-CN',
    })
    await dataI18nService.batchSave([
      {
        entityType: 'article',
        entityId: 'A-1',
        fieldName: 'title',
        locale: 'zh-CN',
        value: 'Title CN',
        tenantId: 'tenant-1',
      },
    ])
    const resolved = await dataI18nService.resolve({
      entityType: 'article',
      entityId: 'A-1',
      fieldName: 'title',
      locale: 'en-US',
      tenantId: 'tenant-1',
      fallbackLocale: 'zh-CN',
      originalValue: 'Original',
    })

    const params = mockedGet.mock.calls[0][1]?.params as URLSearchParams
    expect(mockedGet.mock.calls[0][0]).toBe('/v1/infra/data-i18n/translations')
    expect(params.get('tenantId')).toBe('tenant-1')
    expect(params.get('entityType')).toBe('article')
    expect(mockedPost).toHaveBeenCalledWith(
      '/v1/infra/data-i18n/translations/batch',
      {
        entries: [
          {
            entityType: 'article',
            entityId: 'A-1',
            fieldName: 'title',
            locale: 'zh-CN',
            value: 'Title CN',
            tenantId: 'tenant-1',
          },
        ],
      },
      expect.objectContaining({
        dedupeKey: expect.stringContaining('data-i18n:batch'),
      }),
    )
    expect(resolved.resolveSource).toBe('FALLBACK')
  })

  it('writes timezone settings and conversion through scoped endpoints', async () => {
    mockedPut.mockResolvedValueOnce({
      id: 'tz-1',
      scopeType: 'TENANT',
      scopeId: 'tenant-1',
      timezoneId: 'Asia/Shanghai',
      isDefault: false,
      active: true,
    })
    mockedPost.mockResolvedValueOnce({
      localDateTime: '2026-04-29T16:00:00',
      timezoneId: 'Asia/Shanghai',
    })

    await timezoneService.setTenantTimezone('tenant-1', 'Asia/Shanghai')
    const converted = await timezoneService.convertFromUtc(
      '2026-04-29T08:00:00Z',
      'Asia/Shanghai',
    )

    expect(mockedPut).toHaveBeenCalledWith(
      '/v1/infra/timezone/settings/tenant/tenant-1',
      { timezoneId: 'Asia/Shanghai' },
      { dedupeKey: 'timezone:tenant:tenant-1' },
    )
    expect(converted.localDateTime).toBe('2026-04-29T16:00:00')
  })

  it('uses backend localized messages before frontend fallback text', async () => {
    mockedGet.mockResolvedValueOnce([
      {
        id: 'err-1',
        code: 'BAD_REQUEST',
        moduleCode: 'shared',
        severity: 'WARN',
        httpStatus: 400,
        messageKey: 'shared.bad_request',
        message: 'Localized bad request',
        retryable: false,
        deprecated: false,
      },
    ])

    const page = await errorCodeService.list({ moduleCode: 'shared' })
    const error = new BizError({
      code: 'BAD_REQUEST',
      status: 400,
      backendMessage: page.items[0].message,
    })

    const params = mockedGet.mock.calls[0][1]?.params as URLSearchParams
    expect(mockedGet.mock.calls[0][0]).toBe('/v1/infra/error-codes')
    expect(params.get('moduleCode')).toBe('shared')
    expect(error.message).toBe('Localized bad request')
  })
})
