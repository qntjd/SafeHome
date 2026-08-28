import { ShieldCheck, Bell, Newspaper } from 'lucide-react'

const BADGES = [
  { Icon: ShieldCheck, left: '15.6%', top: '26.6%', rotate: '-8deg', bg: 'var(--accent-blue)', color: '#fff', shadow: 'rgba(11,110,130,0.35)', duration: '5.4s', delay: '0s' },
  { Icon: Bell, left: '85.2%', top: '32%', rotate: '7deg', bg: 'var(--accent-amber)', color: 'var(--ink)', shadow: 'rgba(232,163,61,0.4)', duration: '6s', delay: '0.6s' },
  { Icon: Newspaper, left: '23.4%', top: '78.9%', rotate: '6deg', bg: 'var(--ink)', color: 'var(--accent-amber)', shadow: 'rgba(14,21,38,0.3)', duration: '5.8s', delay: '1.1s' },
]

export default function SafetyIllustration({ className = '' }: { className?: string }) {
  return (
    <div className={`relative w-full aspect-square ${className}`}>
      <svg viewBox="0 0 640 640" className="absolute inset-0 w-full h-full" aria-hidden="true">
        {/* 배경 글로우 */}
        <circle cx="160" cy="160" r="220" fill="var(--accent-amber)" opacity="0.07" />
        <circle cx="480" cy="480" r="240" fill="var(--accent-blue)" opacity="0.07" />
        <circle cx="320" cy="320" r="270" fill="var(--bg-secondary)" opacity="0.7" />

        {/* 배지 → 폰 연결선 */}
        <path d="M118,182 C 150,190 172,196 192,207" fill="none" stroke="var(--accent-blue)" strokeWidth="2" strokeDasharray="4 6" opacity="0.55" />
        <circle cx="192" cy="207" r="3.5" fill="var(--accent-blue)" />
        <path d="M522,212 C 495,214 470,218 450,224" fill="none" stroke="var(--accent-amber)" strokeWidth="2" strokeDasharray="4 6" opacity="0.55" />
        <circle cx="450" cy="224" r="3.5" fill="var(--accent-amber)" />
        <path d="M172,492 C 188,483 197,477 206,468" fill="none" stroke="var(--ink-text-muted)" strokeWidth="2" strokeDasharray="4 6" opacity="0.5" />
        <circle cx="206" cy="468" r="3.5" fill="var(--text-muted)" />

        {/* 폰 목업 */}
        <rect x="190" y="120" width="260" height="400" rx="36" fill="var(--ink)" />
        <circle cx="320" cy="136" r="3.5" fill="var(--ink-border)" />
        <rect x="206" y="150" width="228" height="300" rx="18" fill="var(--bg-primary)" />
        <rect x="290" y="493" width="60" height="5" rx="2.5" fill="var(--ink-border)" />

        {/* 화면 속 안전지도 */}
        <rect x="224" y="172" width="46" height="34" rx="4" fill="var(--border)" opacity="0.7" />
        <rect x="360" y="190" width="40" height="28" rx="4" fill="var(--border)" opacity="0.5" />
        <path d="M206,262 H434" stroke="var(--border-hover)" strokeWidth="3" opacity="0.7" />
        <path d="M262,150 V450" stroke="var(--border-hover)" strokeWidth="3" opacity="0.5" />

        {/* 안심 귀가 경로 */}
        <path d="M330,245 C 314,300 300,352 320,410" fill="none" stroke="var(--accent-blue)" strokeWidth="2.5" strokeDasharray="5 6" />
        <circle cx="320" cy="410" r="5" fill="var(--accent-blue)" stroke="var(--bg-primary)" strokeWidth="2" />

        {/* 안전 등급 배지 */}
        <rect x="352" y="214" width="40" height="24" rx="12" fill="var(--grade-a-bg)" stroke="var(--grade-a)" strokeWidth="1.5" />
        <text x="372" y="231" textAnchor="middle" fontSize="13" fontWeight="800" fill="var(--grade-a)" fontFamily="var(--font-display)">A</text>

        {/* 지도 핀 (비콘 신호) */}
        <circle className="pin-ring" cx="330" cy="235" r="10" fill="none" stroke="var(--accent-amber)" strokeWidth="2" />
        <circle className="pin-ring pin-ring--delay" cx="330" cy="235" r="10" fill="none" stroke="var(--accent-amber)" strokeWidth="2" />
        <circle cx="330" cy="235" r="8" fill="var(--accent-amber)" stroke="var(--ink)" strokeWidth="2" />
        <circle cx="330" cy="235" r="3" fill="var(--bg-primary)" />
      </svg>

      {/* 부유 배지 (실제 아이콘 라이브러리 사용) */}
      {BADGES.map(({ Icon, left, top, rotate, bg, color, shadow, duration, delay }, i) => (
        <div key={i} style={{ position: 'absolute', left, top, transform: 'translate(-50%, -50%)' }}>
          <div className="animate-float" style={{ rotate, animationDuration: duration, animationDelay: delay }}>
            <div
              className="w-14 h-14 rounded-2xl flex items-center justify-center"
              style={{ background: bg, boxShadow: `0 10px 24px ${shadow}` }}
            >
              <Icon size={24} strokeWidth={2} color={color} />
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
