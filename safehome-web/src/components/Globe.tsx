import { useEffect, useRef } from 'react'
import createGlobe from 'cobe'

// 서비스 커버리지 도시 — 서울/부산/대구/인천을 점으로 표시
const MARKERS: { location: [number, number]; size: number }[] = [
  { location: [37.5665, 126.978], size: 0.09 },
  { location: [35.1796, 129.0756], size: 0.05 },
  { location: [35.8714, 128.6014], size: 0.04 },
  { location: [37.4563, 126.7052], size: 0.04 },
]

// cobe는 전달받은 canvas를 자기 wrapper div로 직접 감싸버리므로(React가 모르는 DOM 변경),
// canvas를 JSX로 렌더링하지 않고 effect 안에서 직접 만들어 이 container에 붙인다.
// 그래야 StrictMode의 mount→cleanup→mount 이중 실행에서도 React가 관리하는 DOM 구조와
// cobe가 만든 실제 DOM 구조가 어긋나 "removeChild" 에러가 나는 일이 없다.
export default function Globe({ className = '', size = 260 }: { className?: string; size?: number }) {
  const containerRef = useRef<HTMLDivElement>(null)
  const phiRef = useRef(4.5)

  useEffect(() => {
    const container = containerRef.current
    if (!container) return

    const canvas = document.createElement('canvas')
    canvas.setAttribute('aria-hidden', 'true')
    canvas.style.width = `${size}px`
    canvas.style.height = `${size}px`
    canvas.style.aspectRatio = '1'
    canvas.style.opacity = '0'
    canvas.style.transition = 'opacity 0.8s ease'
    container.appendChild(canvas)

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    const dpr = Math.min(window.devicePixelRatio || 1, 2)
    let loaded = false

    const globe = createGlobe(canvas, {
      devicePixelRatio: dpr,
      width: size,
      height: size,
      phi: phiRef.current,
      theta: 0.32,
      dark: 1,
      diffuse: 1.2,
      mapSamples: 14000,
      mapBrightness: 5,
      baseColor: [0.09, 0.13, 0.22],
      markerColor: [0.91, 0.64, 0.24],
      glowColor: [0.14, 0.43, 0.51],
      opacity: 0.9,
      markers: MARKERS,
      onRender: (state) => {
        if (!prefersReducedMotion) {
          phiRef.current += 0.0032
        }
        state.phi = phiRef.current
        if (!loaded) {
          loaded = true
          canvas.style.opacity = '1'
        }
      },
    })

    return () => {
      globe.destroy()
      container.innerHTML = ''
    }
  }, [size])

  return <div ref={containerRef} className={className} style={{ width: size, height: size }} />
}
