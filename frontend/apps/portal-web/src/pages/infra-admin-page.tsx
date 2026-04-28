import type { ReactElement } from 'react'
import {
  Database,
  RadioTower,
  ShieldCheck,
  SlidersHorizontal,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

const COPY = {
  titleKey: 'infra.admin.title',
  titleText: '系统管理',
  descriptionKey: 'infra.admin.description',
  descriptionText: '聚合查看平台配置、接口集成与治理能力的运行摘要。',
} as const

const healthItems = [
  {
    key: 'infra.admin.health.integration',
    title: '集成连接器',
    value: '8 / 8 正常',
    icon: RadioTower,
  },
  {
    key: 'infra.admin.health.data',
    title: '数据同步任务',
    value: '3 个计划任务已启用',
    icon: Database,
  },
  {
    key: 'infra.admin.health.security',
    title: '安全策略',
    value: '身份切换与审计链路正常',
    icon: ShieldCheck,
  },
  {
    key: 'infra.admin.health.config',
    title: '平台配置',
    value: '配置中心与字典项已联通',
    icon: SlidersHorizontal,
  },
] as const

export default function InfraAdminPage(): ReactElement {
  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <Badge>{COPY.titleText}</Badge>
          <CardTitle className="mt-3 text-2xl">{COPY.titleText}</CardTitle>
          <CardDescription className="mt-2 text-base">
            {COPY.descriptionText}
          </CardDescription>
        </CardHeader>
      </Card>

      <div className="grid gap-4 md:grid-cols-2">
        {healthItems.map((healthItem) => {
          const Icon = healthItem.icon

          return (
            <Card key={healthItem.key}>
              <CardContent className="flex items-start gap-4 p-6">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-sky-50 text-sky-600">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-slate-950">
                    {healthItem.title}
                  </h2>
                  <p className="mt-2 text-sm text-slate-500">
                    {healthItem.value}
                  </p>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>
    </div>
  )
}
