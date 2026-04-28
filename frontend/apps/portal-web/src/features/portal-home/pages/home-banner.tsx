import { useMemo, useState, type ReactElement } from 'react'
import { ArrowRight, ChevronLeft, ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import type { PortalBanner } from '@/features/portal-home/types/portal-home'
import { cn } from '@/utils/cn'

const COPY = {
  fallbackTitleKey: 'portal.home.banner.fallbackTitle',
  fallbackTitleText: '统一办公门户',
  fallbackSubtitleKey: 'portal.home.banner.fallbackSubtitle',
  fallbackSubtitleText: '聚合待办、公告、消息和常用入口，集中处理今日工作。',
  previousKey: 'portal.home.banner.previous',
  previousText: '上一张',
  nextKey: 'portal.home.banner.next',
  nextText: '下一张',
} as const

const fallbackBanner: PortalBanner = {
  id: 'fallback',
  title: COPY.fallbackTitleText,
  subtitle: COPY.fallbackSubtitleText,
}

export interface HomeBannerProps {
  banners: PortalBanner[]
}

export default function HomeBanner({ banners }: HomeBannerProps): ReactElement {
  const orderedBanners = useMemo(
    () =>
      banners.length > 0
        ? [...banners].sort(
            (first, second) => (first.priority ?? 0) - (second.priority ?? 0),
          )
        : [fallbackBanner],
    [banners],
  )
  const [activeIndex, setActiveIndex] = useState(0)
  const activeBanner =
    orderedBanners[Math.min(activeIndex, orderedBanners.length - 1)]

  function move(offset: number): void {
    setActiveIndex((currentIndex) => {
      const nextIndex = currentIndex + offset

      if (nextIndex < 0) {
        return orderedBanners.length - 1
      }

      if (nextIndex >= orderedBanners.length) {
        return 0
      }

      return nextIndex
    })
  }

  return (
    <section className="relative overflow-hidden rounded-2xl border border-slate-200 bg-slate-950 text-white shadow-sm">
      {activeBanner.imageUrl ? (
        <img
          alt=""
          className="absolute inset-0 h-full w-full object-cover opacity-55"
          src={activeBanner.imageUrl}
        />
      ) : (
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(56,189,248,0.34),transparent_30%),linear-gradient(135deg,#0f172a,#334155)]" />
      )}
      <div className="relative z-10 grid min-h-[260px] content-between gap-8 p-6 md:p-8">
        <div className="max-w-3xl">
          <p className="text-sm font-medium text-sky-100">
            HJO2OA Portal Workspace
          </p>
          <h2 className="mt-4 text-3xl font-semibold leading-tight md:text-4xl">
            {activeBanner.title}
          </h2>
          {activeBanner.subtitle ? (
            <p className="mt-3 max-w-2xl text-base text-slate-100">
              {activeBanner.subtitle}
            </p>
          ) : null}
          {activeBanner.actionHref && activeBanner.actionText ? (
            <Link
              className="mt-6 inline-flex h-10 items-center gap-2 rounded-xl bg-white px-4 text-sm font-medium text-slate-950 hover:bg-slate-100"
              to={activeBanner.actionHref}
            >
              {activeBanner.actionText}
              <ArrowRight className="h-4 w-4" />
            </Link>
          ) : null}
        </div>
        <div className="flex items-center justify-between gap-4">
          <div className="flex gap-2">
            {orderedBanners.map((banner, index) => (
              <button
                aria-label={`${COPY.nextText} ${index + 1}`}
                className={cn(
                  'h-2.5 rounded-full transition-all',
                  index === activeIndex
                    ? 'w-8 bg-white'
                    : 'w-2.5 bg-white/45 hover:bg-white/70',
                )}
                key={banner.id}
                onClick={() => setActiveIndex(index)}
                type="button"
              />
            ))}
          </div>
          {orderedBanners.length > 1 ? (
            <div className="flex gap-2">
              <Button
                aria-label={COPY.previousText}
                className="bg-white/10 text-white hover:bg-white/20"
                onClick={() => move(-1)}
                size="icon"
                variant="ghost"
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                aria-label={COPY.nextText}
                className="bg-white/10 text-white hover:bg-white/20"
                onClick={() => move(1)}
                size="icon"
                variant="ghost"
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}
