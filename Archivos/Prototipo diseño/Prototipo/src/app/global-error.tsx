'use client';

import { Button } from '@/components/ui/button';
import { AlertTriangle } from 'lucide-react';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="en">
      <body className="font-body antialiased">
        <div className="flex flex-col items-center justify-center min-h-screen bg-background text-foreground p-4">
          <div className="text-center max-w-lg p-8 border rounded-lg shadow-xl bg-card">
            <AlertTriangle className="mx-auto h-16 w-16 text-destructive mb-4" />
            <h1 className="text-3xl font-bold text-destructive mb-2 font-headline">Oops! Something went wrong.</h1>
            <p className="text-muted-foreground mb-6">
              We encountered an unexpected issue. Please try again.
            </p>
            {process.env.NODE_ENV === 'development' && error?.message && (
              <details className="mb-4 text-left bg-muted p-3 rounded-md">
                <summary className="cursor-pointer font-medium">Error Details (Development)</summary>
                <pre className="mt-2 whitespace-pre-wrap text-xs">
                  {error.message}
                  {error.digest && `\nDigest: ${error.digest}`}
                  {error.stack && `\nStack: ${error.stack}`}
                </pre>
              </details>
            )}
            <Button onClick={() => reset()} size="lg">
              Try Again
            </Button>
          </div>
        </div>
      </body>
    </html>
  );
}
