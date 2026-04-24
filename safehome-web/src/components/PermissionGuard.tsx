import { useState } from 'react'
import { usePermissions } from '@/hooks/usePermissions'

interface Props {
  children: React.ReactNode
}

export default function PermissionGuard({ children }: Props) {
  const { locationStatus, notificationStatus, requestLocation, requestNotification } = usePermissions()
  const [step, setStep] = useState<'intro' | 'done'>('intro')
  const [loading, setLoading] = useState(false)

  const isAllGranted =
    locationStatus === 'granted' && notificationStatus === 'granted'

  const isAllDeniedOrGranted =
    (locationStatus === 'granted' || locationStatus === 'denied') &&
    (notificationStatus === 'granted' || notificationStatus === 'denied')

  if (step === 'done' || isAllGranted || isAllDeniedOrGranted) {
    return <>{children}</>
  }

  const handleAllow = async () => {
    setLoading(true)
    await requestLocation()
    await requestNotification()
    setLoading(false)
    setStep('done')
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full opacity-10 blur-3xl pointer-events-none"
        style={{ background: 'var(--accent-blue)' }}
      />

      <div className="w-full max-w-sm relative">
        {/* 로고 */}
        <div className="text-center mb-8">
          <div
            className="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl font-bold mx-auto mb-4"
            style={{ background: 'var(--accent-blue)' }}
          >
            S
          </div>
          <h1 className="text-xl font-bold mb-1" style={{ color: 'var(--text-primary)' }}>
            SafeHome 시작하기
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            더 나은 서비스를 위해 아래 권한이 필요해요
          </p>
        </div>

        {/* 권한 카드 */}
        <div
          className="rounded-2xl p-5 mb-4 flex flex-col gap-4"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
        >
          {/* 위치 권한 */}
          <div className="flex items-start gap-4">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 text-lg"
              style={{ background: 'rgba(79,126,248,0.15)' }}
            >
              📍
            </div>
            <div className="flex-1">
              <div className="flex items-center justify-between mb-0.5">
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                  위치 권한
                </p>
                <StatusBadge status={locationStatus} />
              </div>
              <p className="text-xs leading-relaxed" style={{ color: 'var(--text-muted)' }}>
                주변 안전시설 조회, 안심 귀가 경로 설정, 동네 안전점수 확인에 사용돼요.
              </p>
            </div>
          </div>

          <div style={{ height: 1, background: 'var(--border)' }} />

          {/* 알림 권한 */}
          <div className="flex items-start gap-4">
            <div
              className="w-10 h-10 rounded-xl flex items-center justify-center shrink-0 text-lg"
              style={{ background: 'rgba(251,191,36,0.15)' }}
            >
              🔔
            </div>
            <div className="flex-1">
              <div className="flex items-center justify-between mb-0.5">
                <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                  알림 권한
                </p>
                <StatusBadge status={notificationStatus} />
              </div>
              <p className="text-xs leading-relaxed" style={{ color: 'var(--text-muted)' }}>
                재난문자, 범죄 발생 경보, 안심 귀가 미도착 알림을 실시간으로 받아요.
              </p>
            </div>
          </div>
        </div>

        {/* 개인정보 안내 */}
        <p className="text-xs text-center mb-4" style={{ color: 'var(--text-muted)' }}>
          위치 정보는 서비스 제공 목적으로만 사용되며<br />
          외부에 공유되지 않아요.
        </p>

        {/* 버튼 */}
        <button
          onClick={handleAllow}
          disabled={loading}
          className="w-full rounded-xl py-3 text-sm font-medium transition-all disabled:opacity-50 mb-3"
          style={{ background: 'var(--accent-blue)', color: '#fff' }}
        >
          {loading ? '권한 요청 중...' : '권한 허용하고 시작하기'}
        </button>

        <button
          onClick={() => setStep('done')}
          className="w-full rounded-xl py-2.5 text-sm transition-all"
          style={{ color: 'var(--text-muted)', border: '1px solid var(--border)' }}
        >
          나중에 설정할게요
        </button>
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const config = {
    idle:       { label: '대기',   color: 'var(--text-muted)',   bg: 'var(--bg-hover)' },
    requesting: { label: '요청중', color: 'var(--accent-amber)', bg: 'rgba(251,191,36,0.1)' },
    granted:    { label: '허용됨', color: 'var(--accent-green)', bg: 'rgba(52,211,153,0.1)' },
    denied:     { label: '거부됨', color: 'var(--accent-red)',   bg: 'rgba(248,113,113,0.1)' },
  }
  const cfg = config[status as keyof typeof config] ?? config.idle

  return (
    <span
      className="text-xs px-2 py-0.5 rounded-full font-medium"
      style={{ color: cfg.color, background: cfg.bg }}
    >
      {cfg.label}
    </span>
  )
}