import type React from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  children?: React.ReactNode; // For actions like "Add New" button
}

export function PageHeader({ title, description, children }: PageHeaderProps) {
  return (
    <div className="mb-6 flex flex-col items-start gap-4 md:flex-row md:items-center md:justify-between">
      <div>
        <h1 className="font-headline text-3xl font-bold tracking-tight text-primary">{title}</h1>
        {description && <p className="text-muted-foreground">{description}</p>}
      </div>
      {children && <div className="ml-auto flex items-center gap-2">{children}</div>}
    </div>
  );
}
