export default function Footer() {
  return (
    <footer style={{ background: 'var(--ink)', borderTop: '1px solid var(--ink-border)' }}>
      {/* 상단 */}
      <div className="px-6 py-8" style={{ borderBottom: '1px solid var(--ink-border)' }}>
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row gap-8 sm:gap-0 justify-between">

          {/* 로고 & 소개 */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2 mb-1">
              <div
                className="w-7 h-7 rounded-full flex items-center justify-center"
                style={{ background: 'var(--accent-amber)' }}
              >
                <span className="font-display font-black text-sm" style={{ color: 'var(--ink)' }}>S</span>
              </div>
              <span className="font-display font-extrabold text-sm tracking-tight" style={{ color: 'var(--ink-text)' }}>
                SafeHome
              </span>
            </div>
            <p className="text-xs leading-relaxed" style={{ color: 'var(--ink-text-muted)', maxWidth: 280 }}>
              1인 가구를 위한 안심 생활 플랫폼으로<br />
              실시간 안전정보와 귀가 안심 서비스를 제공합니다.
            </p>
          </div>

          {/* 데이터 출처 */}
          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold mb-1" style={{ color: 'var(--ink-text)' }}>데이터 출처</p>
            {[
              { label: '경찰청',              desc: '범죄 발생 지역별 통계' },
              { label: '행정안전부',           desc: 'CCTV·비상벨 위치 정보' },
              { label: '한국형사법무정책연구원', desc: '범죄통계정보' },
              { label: '네이버',              desc: '안전 뉴스 검색' },
            ].map(({ label, desc }) => (
              <div key={label} className="flex items-center gap-2">
                <span className="beacon-dot beacon-dot--static" style={{ width: 4, height: 4 }} />
                <span className="text-xs" style={{ color: 'var(--ink-text-muted)' }}>
                  <span style={{ color: 'var(--ink-text)' }}>{label}</span> · {desc}
                </span>
              </div>
            ))}
          </div>

          {/* 관련 사이트 */}
          <div className="flex flex-col gap-2">
            <p className="text-xs font-semibold mb-1" style={{ color: 'var(--ink-text)' }}>관련 사이트</p>
            {[
              { label: '공공데이터포털', url: 'https://www.data.go.kr' },
              { label: '경찰청',        url: 'https://www.police.go.kr' },
              { label: '행정안전부',    url: 'https://www.mois.go.kr' },
              { label: '생활안전지도',  url: 'https://www.safemap.go.kr' },
            ].map(({ label, url }) => (
              <a
                key={label}
                href={url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs transition-colors"
                style={{ color: 'var(--ink-text-muted)' }}
                onMouseEnter={e => (e.currentTarget.style.color = 'var(--accent-amber)')}
                onMouseLeave={e => (e.currentTarget.style.color = 'var(--ink-text-muted)')}
              >
                {label} →
              </a>
            ))}
          </div>
        </div>
      </div>

      {/* 하단 — 저작권 & 면책 */}
      <div className="px-6 py-4">
        <div className="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2">
          <p className="text-xs font-mono" style={{ color: 'var(--ink-text-muted)' }}>
            © 2026 SafeHome
          </p>
          <p className="text-xs text-center" style={{ color: 'var(--ink-text-muted)' }}>
            본 시스템의 안전점수 및 범죄통계는 경찰청·행정안전부 공공데이터를 기반으로 산출되며, 실제 치안 상황과 다를 수 있습니다.
          </p>
        </div>
      </div>
    </footer>
  )
}
