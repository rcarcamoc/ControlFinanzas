import type { SVGProps } from 'react';

export function Logo(props: SVGProps<SVGSVGElement>) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 200 50"
      width="120"
      height="30"
      aria-label="FinaVision Logo"
      {...props}
    >
      <defs>
        <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" style={{ stopColor: 'hsl(var(--primary))', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: 'hsl(var(--accent))', stopOpacity: 1 }} />
        </linearGradient>
      </defs>
      <path d="M10 40 Q15 10 30 25 T50 40" stroke="url(#logoGradient)" strokeWidth="5" fill="none" />
      <path d="M30 15 Q40 40 55 20 T80 35" stroke="url(#logoGradient)" strokeWidth="5" fill="none" />
      <text
        x="90"
        y="35"
        fontFamily="PT Sans, sans-serif"
        fontSize="30"
        fontWeight="bold"
        fill="hsl(var(--primary))"
      >
        FinaVision
      </text>
    </svg>
  );
}
