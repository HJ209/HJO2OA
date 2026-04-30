import type { ChangeEvent, FormEvent, ReactElement } from 'react'
import { useMemo, useState } from 'react'
import { Download, Eye, FileUp, History, Upload } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { InfraPageSection } from '@/features/infra-admin/components/infra-page-section'
import {
  InfraTable,
  StatusPill,
} from '@/features/infra-admin/components/infra-table'
import { useAttachmentAssets } from '@/features/infra-admin/hooks/use-attachment'
import { attachmentService } from '@/features/infra-admin/services/attachment-service'
import type {
  AttachmentAsset,
  AttachmentBinding,
  AttachmentPreview,
} from '@/features/infra-admin/types/infra'

export default function AttachmentPage(): ReactElement {
  const queryClient = useQueryClient()
  const query = useAttachmentAssets({ page: 1, size: 20 })
  const assets = useMemo(() => query.data?.items ?? [], [query.data?.items])
  const [file, setFile] = useState<File | null>(null)
  const [businessType, setBusinessType] = useState('DOC')
  const [businessId, setBusinessId] = useState('doc-001')
  const [bindingRole, setBindingRole] =
    useState<AttachmentBinding['bindingRole']>('ATTACHMENT')
  const [selectedAssetId, setSelectedAssetId] = useState('')
  const [versionFile, setVersionFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<AttachmentPreview | null>(null)
  const selectedAsset = useMemo(
    () => assets.find((asset) => asset.id === selectedAssetId) ?? assets[0],
    [assets, selectedAssetId],
  )

  const refresh = () =>
    queryClient.invalidateQueries({ queryKey: ['infra', 'attachments'] })

  const upload = useMutation({
    mutationFn: () => {
      if (!file) {
        throw new Error('No file selected')
      }

      return attachmentService.upload({
        file,
        businessType,
        businessId,
        bindingRole,
      })
    },
    onSuccess: (asset) => {
      setSelectedAssetId(asset.id)
      setFile(null)
      refresh()
    },
  })
  const uploadVersion = useMutation({
    mutationFn: () => {
      if (!selectedAsset || !versionFile) {
        throw new Error('No attachment version selected')
      }

      return attachmentService.uploadVersion(selectedAsset.id, versionFile)
    },
    onSuccess: () => {
      setVersionFile(null)
      refresh()
    },
  })

  function onFileChange(event: ChangeEvent<HTMLInputElement>): void {
    setFile(event.target.files?.[0] ?? null)
  }

  function onVersionFileChange(event: ChangeEvent<HTMLInputElement>): void {
    setVersionFile(event.target.files?.[0] ?? null)
  }

  function submitUpload(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault()
    upload.mutate()
  }

  async function download(
    asset: AttachmentAsset,
    versionNo?: number,
  ): Promise<void> {
    await attachmentService.download(asset.id, versionNo)
  }

  async function loadPreview(asset: AttachmentAsset): Promise<void> {
    setSelectedAssetId(asset.id)
    setPreview(await attachmentService.preview(asset.id))
  }

  return (
    <InfraPageSection
      description="真实文件上传、版本、下载、预览和绑定关系。"
      title="附件资产"
    >
      <form
        className="mb-5 grid gap-3 rounded-lg border border-slate-200 bg-white p-4 md:grid-cols-6"
        onSubmit={submitUpload}
      >
        <label className="flex items-center gap-2 rounded-md border border-dashed border-slate-300 px-3 py-2 text-sm md:col-span-2">
          <FileUp size={16} />
          <input
            className="min-w-0 flex-1 text-sm"
            onChange={onFileChange}
            type="file"
          />
        </label>
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) => setBusinessType(event.target.value)}
          value={businessType}
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) => setBusinessId(event.target.value)}
          value={businessId}
        />
        <select
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          onChange={(event) =>
            setBindingRole(
              event.target.value as AttachmentBinding['bindingRole'],
            )
          }
          value={bindingRole}
        >
          <option value="ATTACHMENT">ATTACHMENT</option>
          <option value="PRIMARY">PRIMARY</option>
          <option value="COVER">COVER</option>
          <option value="PREVIEW_SOURCE">PREVIEW_SOURCE</option>
        </select>
        <button
          className="inline-flex items-center justify-center gap-2 rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white"
          disabled={!file || upload.isPending}
          type="submit"
        >
          <Upload size={16} />
          上传
        </button>
      </form>

      <InfraTable
        columns={[
          {
            key: 'originalFilename',
            title: '文件名',
            render: (item) => item.originalFilename,
          },
          {
            key: 'contentType',
            title: '类型',
            render: (item) => item.contentType,
          },
          {
            key: 'sizeBytes',
            title: '大小',
            render: (item) => `${Math.ceil(item.sizeBytes / 1024)} KB`,
          },
          {
            key: 'previewStatus',
            title: '预览',
            render: (item) => (
              <StatusPill active={item.previewStatus === 'READY'}>
                {item.previewStatus}
              </StatusPill>
            ),
          },
          {
            key: 'latestVersionNo',
            title: '版本',
            render: (item) => `v${item.latestVersionNo}`,
          },
          {
            key: 'actions',
            title: '操作',
            render: (item) => (
              <div className="flex flex-wrap gap-2">
                <button
                  className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2 py-1 text-xs"
                  onClick={() => void download(item)}
                  type="button"
                >
                  <Download size={14} />
                  下载
                </button>
                <button
                  className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2 py-1 text-xs"
                  onClick={() => void loadPreview(item)}
                  type="button"
                >
                  <Eye size={14} />
                  预览
                </button>
                <button
                  className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2 py-1 text-xs"
                  onClick={() => setSelectedAssetId(item.id)}
                  type="button"
                >
                  <History size={14} />
                  版本
                </button>
              </div>
            ),
          },
        ]}
        getRowKey={(item) => item.id}
        isLoading={query.isLoading}
        items={assets}
      />

      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <section className="rounded-lg border border-slate-200 bg-white p-4">
          <select
            className="mb-3 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
            onChange={(event) => setSelectedAssetId(event.target.value)}
            value={selectedAsset?.id ?? ''}
          >
            {assets.map((asset) => (
              <option key={asset.id} value={asset.id}>
                {asset.originalFilename}
              </option>
            ))}
          </select>
          <div className="mb-3 flex gap-2">
            <input
              className="min-w-0 flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
              onChange={onVersionFileChange}
              type="file"
            />
            <button
              className="inline-flex items-center gap-2 rounded-md bg-slate-900 px-3 py-2 text-sm text-white"
              disabled={
                !selectedAsset || !versionFile || uploadVersion.isPending
              }
              onClick={() => uploadVersion.mutate()}
              type="button"
            >
              <Upload size={16} />
              新版本
            </button>
          </div>
          <div className="space-y-2 text-sm text-slate-700">
            {(selectedAsset?.versions ?? []).map((version) => (
              <div
                className="flex items-center justify-between rounded-md bg-slate-50 px-3 py-2"
                key={version.id}
              >
                <span>
                  v{version.versionNo} · {Math.ceil(version.sizeBytes / 1024)}{' '}
                  KB
                </span>
                <button
                  className="inline-flex items-center gap-1 rounded-md border border-slate-300 px-2 py-1 text-xs"
                  onClick={() =>
                    selectedAsset &&
                    void download(selectedAsset, version.versionNo)
                  }
                  type="button"
                >
                  <Download size={14} />
                  下载
                </button>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-700">
          <div className="mb-2 font-medium">预览状态</div>
          {preview ? (
            <div className="space-y-2">
              <div>{preview.previewStatus}</div>
              <div>
                {preview.previewAvailable ? '可直接预览' : '暂无预览格式'}
              </div>
              <div className="break-all text-xs text-slate-500">
                {preview.downloadUrl}
              </div>
            </div>
          ) : (
            <div className="text-slate-500">-</div>
          )}
          <div className="mt-4 font-medium">绑定关系</div>
          <div className="mt-2 space-y-2">
            {(selectedAsset?.bindings ?? []).map((binding) => (
              <div
                className="rounded-md bg-slate-50 px-3 py-2"
                key={binding.id}
              >
                {binding.businessType}/{binding.businessId} ·{' '}
                {binding.bindingRole} · {binding.active ? 'ACTIVE' : 'INACTIVE'}
              </div>
            ))}
          </div>
        </section>
      </div>
    </InfraPageSection>
  )
}
