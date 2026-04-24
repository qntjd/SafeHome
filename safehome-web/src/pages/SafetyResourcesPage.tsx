import Footer from '@/components/Footer'

const RESOURCES = [
  {
    category: '휴대폰 · 통신',
    items: [
      {
        icon: '📱',
        agency: '과학기술정보통신부',
        agencyColor: '#e0f2fe',
        agencyTextColor: '#0369a1',
        title: '명의도용방지 서비스',
        desc: '내 명의로 개통된 휴대폰 전체 조회 및 신규 개통 차단',
        url: 'https://www.msafer.or.kr',
        urlLabel: 'msafer.or.kr',
      },
    ],
  },
  {
    category: '금융 · 결제',
    items: [
      {
        icon: '💳',
        agency: '금융결제원',
        agencyColor: '#dbeafe',
        agencyTextColor: '#1d4ed8',
        title: '페이인포',
        desc: '내 명의의 카드, 계좌, 간편결제 서비스 한번에 조회',
        url: 'https://www.payinfo.or.kr',
        urlLabel: 'payinfo.or.kr',
      },
      {
        icon: '🏦',
        agency: '한국신용정보원',
        agencyColor: '#fef9c3',
        agencyTextColor: '#854d0e',
        title: '크레딧포유',
        desc: '대출, 보증 등 금융거래 정보 조회 및 명의도용 확인',
        url: 'https://www.creditinfo.or.kr',
        urlLabel: 'creditinfo.or.kr',
      },
    ],
  },
  {
    category: '개인정보 · 공공서비스',
    items: [
      {
        icon: '🔒',
        agency: '개인정보보호위원회',
        agencyColor: '#fce7f3',
        agencyTextColor: '#9d174d',
        title: '개인정보 포털',
        desc: '공공기관의 내 개인정보 열람 및 처리 현황 확인',
        url: 'https://www.privacy.go.kr',
        urlLabel: 'privacy.go.kr',
      },
      {
        icon: '🏛',
        agency: '행정안전부',
        agencyColor: '#ede9fe',
        agencyTextColor: '#5b21b6',
        title: '정부24',
        desc: '공공서비스 가입 및 개인정보 제공 내역 조회',
        url: 'https://www.gov.kr',
        urlLabel: 'gov.kr',
      },
    ],
  },
  {
    category: '안전 · 재난',
    items: [
      {
        icon: '🚨',
        agency: '행정안전부',
        agencyColor: '#fee2e2',
        agencyTextColor: '#991b1b',
        title: '생활안전지도',
        desc: '전국 안전 취약지역 및 범죄 위험 지역 확인',
        url: 'https://www.safemap.go.kr',
        urlLabel: 'safemap.go.kr',
      },
      {
        icon: '🆘',
        agency: '행정안전부',
        agencyColor: '#fee2e2',
        agencyTextColor: '#991b1b',
        title: '안전디딤돌',
        desc: '재난 대피소, 비상연락처, 응급처치 정보 제공',
        url: 'https://www.safekorea.go.kr',
        urlLabel: 'safekorea.go.kr',
      },
    ],
  },
  {
    category: '여성 · 1인가구',
    items: [
      {
        icon: '👩',
        agency: '여성가족부',
        agencyColor: '#fce7f3',
        agencyTextColor: '#9d174d',
        title: '여성긴급전화 1366',
        desc: '24시간 여성폭력 피해 상담 및 긴급 지원',
        url: 'https://www.women1366.kr',
        urlLabel: 'women1366.kr',
      },
      {
        icon: '🏠',
        agency: '국토교통부',
        agencyColor: '#dcfce7',
        agencyTextColor: '#166534',
        title: '마이홈 포털',
        desc: '1인 가구를 위한 공공임대주택 및 주거 지원 정보',
        url: 'https://www.myhome.go.kr',
        urlLabel: 'myhome.go.kr',
      },
    ],
  },
]

export default function SafetyResourcesPage() {
  return (
    <div className="min-h-full pb-20 sm:pb-0" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-3xl mx-auto px-4 py-6 sm:py-8">

        {/* 헤더 */}
        <div className="text-center mb-8">
          <h1 className="text-xl sm:text-2xl font-bold mb-2" style={{ color: 'var(--text-primary)' }}>
            내 개인정보, 내가 지키기
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            공식 기관을 통해 내 명의 사용 현황을 무료로 확인할 수 있어요
          </p>
        </div>

        {/* 카테고리별 카드 */}
        <div className="flex flex-col gap-8">
          {RESOURCES.map(({ category, items }) => (
            <div key={category}>
              <p
                className="text-xs font-medium mb-3 px-1"
                style={{ color: 'var(--text-muted)' }}
              >
                {category}
              </p>
              <div className={`grid gap-3 ${items.length === 1 ? 'grid-cols-1' : 'grid-cols-1 sm:grid-cols-2'}`}>
                {items.map((item) => (
                  <a
                    key={item.title}
                    href={item.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block rounded-2xl p-5 transition-all"
                    style={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                    }}
                    onMouseEnter={e => (e.currentTarget.style.borderColor = 'var(--border-hover)')}
                    onMouseLeave={e => (e.currentTarget.style.borderColor = 'var(--border)')}
                  >
                    {/* 아이콘 */}
                    <div
                      className="w-11 h-11 rounded-xl flex items-center justify-center text-xl mb-3"
                      style={{ background: item.agencyColor }}
                    >
                      {item.icon}
                    </div>

                    {/* 기관 배지 */}
                    <span
                      className="text-xs px-2 py-0.5 rounded-full font-medium"
                      style={{
                        background: item.agencyColor,
                        color: item.agencyTextColor,
                      }}
                    >
                      {item.agency}
                    </span>

                    {/* 제목 & 설명 */}
                    <p className="text-sm font-semibold mt-2 mb-1" style={{ color: 'var(--text-primary)' }}>
                      {item.title}
                    </p>
                    <p className="text-xs leading-relaxed mb-3" style={{ color: 'var(--text-muted)' }}>
                      {item.desc}
                    </p>

                    {/* URL */}
                    <p className="text-xs font-medium" style={{ color: 'var(--accent-blue)' }}>
                      {item.urlLabel} →
                    </p>
                  </a>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* 안내 배너 */}
        <div
          className="mt-8 rounded-2xl px-5 py-4 flex items-start gap-3"
          style={{ background: 'rgba(251,191,36,0.1)', border: '1px solid rgba(251,191,36,0.2)' }}
        >
          <span className="text-lg shrink-0">💡</span>
          <p className="text-xs leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
            모든 서비스는 <strong style={{ color: 'var(--text-primary)' }}>무료</strong>이며 본인인증 후 이용 가능합니다.
            의심스러운 개통이나 금융거래 발견 시 즉시 해당 기관에 신고하세요.
          </p>
        </div>

      </div>
      <Footer />
    </div>
  )
}