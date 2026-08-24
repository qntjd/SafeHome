import { Smartphone, CreditCard, Landmark, Lock, Building2, Siren, LifeBuoy, Users, Home, Lightbulb, ExternalLink } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import Footer from '@/components/Footer'

const RESOURCES: {
  category: string
  items: {
    icon: LucideIcon
    agency: string
    agencyColor: string
    agencyTextColor: string
    title: string
    desc: string
    url: string
    urlLabel: string
  }[]
}[] = [
  {
    category: '휴대폰 · 통신',
    items: [
      {
        icon: Smartphone,
        agency: '과학기술정보통신부',
        agencyColor: 'var(--bg-blue-soft)',
        agencyTextColor: 'var(--accent-blue)',
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
        icon: CreditCard,
        agency: '금융결제원',
        agencyColor: 'var(--grade-b-bg)',
        agencyTextColor: 'var(--grade-b)',
        title: '페이인포',
        desc: '내 명의의 카드, 계좌, 간편결제 서비스 한번에 조회',
        url: 'https://www.payinfo.or.kr',
        urlLabel: 'payinfo.or.kr',
      },
      {
        icon: Landmark,
        agency: '한국신용정보원',
        agencyColor: 'var(--accent-amber-soft)',
        agencyTextColor: 'var(--accent-amber-deep)',
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
        icon: Lock,
        agency: '개인정보보호위원회',
        agencyColor: '#efeafb',
        agencyTextColor: 'var(--accent-purple)',
        title: '개인정보 포털',
        desc: '공공기관의 내 개인정보 열람 및 처리 현황 확인',
        url: 'https://www.privacy.go.kr',
        urlLabel: 'privacy.go.kr',
      },
      {
        icon: Building2,
        agency: '행정안전부',
        agencyColor: 'var(--grade-a-bg)',
        agencyTextColor: 'var(--grade-a)',
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
        icon: Siren,
        agency: '행정안전부',
        agencyColor: 'var(--accent-red-soft)',
        agencyTextColor: 'var(--accent-red)',
        title: '생활안전지도',
        desc: '전국 안전 취약지역 및 범죄 위험 지역 확인',
        url: 'https://www.safemap.go.kr',
        urlLabel: 'safemap.go.kr',
      },
      {
        icon: LifeBuoy,
        agency: '행정안전부',
        agencyColor: 'var(--accent-red-soft)',
        agencyTextColor: 'var(--accent-red)',
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
        icon: Users,
        agency: '여성가족부',
        agencyColor: '#efeafb',
        agencyTextColor: 'var(--accent-purple)',
        title: '여성긴급전화 1366',
        desc: '24시간 여성폭력 피해 상담 및 긴급 지원',
        url: 'https://www.women1366.kr',
        urlLabel: 'women1366.kr',
      },
      {
        icon: Home,
        agency: '국토교통부',
        agencyColor: 'var(--grade-a-bg)',
        agencyTextColor: 'var(--grade-a)',
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
          <h1 className="font-display font-black text-xl sm:text-2xl mb-2" style={{ color: 'var(--text-primary)' }}>
            내 개인정보, 내가 지키기
          </h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            공식 기관을 통해 내 명의 사용 현황을 무료로 확인할 수 있어요
          </p>
        </div>

        {/* 카테고리별 리스트 — 아이콘 원형 칩 + 리스트-로우 (파스텔 카드 그리드 지양) */}
        <div className="flex flex-col gap-6">
          {RESOURCES.map(({ category, items }) => (
            <div key={category}>
              <p
                className="text-xs font-medium mb-3 px-1"
                style={{ color: 'var(--text-muted)' }}
              >
                {category}
              </p>
              <div
                className="rounded-2xl overflow-hidden"
                style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
              >
                {items.map((item, i) => {
                  const Icon = item.icon
                  return (
                    <a
                      key={item.title}
                      href={item.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-start gap-3 px-4 py-3.5 transition-colors"
                      style={{ borderTop: i > 0 ? '1px solid var(--border)' : undefined }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      {/* 아이콘 — 중립 원형 칩 + stroke만 색상 */}
                      <div
                        className="w-11 h-11 rounded-full flex items-center justify-center shrink-0"
                        style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)' }}
                      >
                        <Icon size={20} strokeWidth={2} color={item.agencyTextColor} />
                      </div>

                      <div className="flex-1 min-w-0">
                        {/* 기관 배지 */}
                        <span
                          className="inline-block text-xs px-2 py-0.5 rounded-full font-medium mb-1.5"
                          style={{ background: item.agencyColor, color: item.agencyTextColor }}
                        >
                          {item.agency}
                        </span>

                        {/* 제목 & 설명 */}
                        <p className="font-display font-bold text-sm mb-1" style={{ color: 'var(--text-primary)' }}>
                          {item.title}
                        </p>
                        <p className="text-xs leading-relaxed mb-1.5" style={{ color: 'var(--text-muted)' }}>
                          {item.desc}
                        </p>
                        <p className="text-xs font-medium" style={{ color: 'var(--accent-blue)' }}>
                          {item.urlLabel}
                        </p>
                      </div>

                      <ExternalLink size={15} color="var(--text-muted)" className="shrink-0 mt-1" />
                    </a>
                  )
                })}
              </div>
            </div>
          ))}
        </div>

        {/* 안내 배너 */}
        <div
          className="mt-8 rounded-2xl px-5 py-4 flex items-start gap-3"
          style={{ background: 'var(--accent-amber-soft)', border: '1px solid var(--accent-amber)' }}
        >
          <Lightbulb size={18} strokeWidth={2} color="var(--accent-amber-deep)" className="shrink-0" />
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
