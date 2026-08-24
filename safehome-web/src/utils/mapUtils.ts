// 마커 색상은 디자인 시스템 CSS 변수(src/index.css)를 런타임에 읽어와 사용한다.
// (지도 마커는 data URI 이미지로 렌더링되어 CSS 변수 문자열을 직접 쓸 수 없으므로,
//  getComputedStyle로 실제 값을 읽고, 변수 정의와 동일한 값을 폴백으로 둔다.)
const cssVar = (name: string, fallback: string) => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return fallback
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return value || fallback
}

export const getFacilityMarkerImage = (type: string) => {
  const colors: Record<string, string> = {
    CCTV:           cssVar('--accent-blue', '#0b6e82'),
    EMERGENCY_BELL: cssVar('--accent-red', '#d7263d'),
    STREETLIGHT:    cssVar('--accent-amber', '#e8a33d'),
    POLICE:         cssVar('--accent-purple', '#7c5cbf'),
  }
  const color = colors[type] ?? cssVar('--text-muted', '#8996ac')
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36"><path d="M14 0C6.268 0 0 6.268 0 14c0 9.333 14 22 14 22S28 23.333 28 14C28 6.268 21.732 0 14 0z" fill="${color}" stroke="white" stroke-width="2"/><circle cx="14" cy="14" r="6" fill="white"/></svg>`
  return {
    src:  'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(svg))),
    size: { width: 28, height: 36 },
  }
}
