'use client';

import { useState, useMemo, type MouseEvent } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { StatCard } from '@/components/shared/StatCard';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { ChartContainer, ChartTooltip, ChartTooltipContent, ChartLegend, ChartLegendContent } from '@/components/ui/chart';
import { BarChart, CartesianGrid, XAxis, YAxis, Bar, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { DollarSign, TrendingUp, Target, Users, RefreshCw, ArrowLeft, BarChartHorizontalBig, ListTree } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { SAMPLE_CATEGORIES, SAMPLE_BUDGETS, SAMPLE_TRANSACTIONS, type Category, type Budget, type Transaction } from '@/lib/constants';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';


// Helper to get category name
const getCategoryName = (categoryId: string, categories: Category[]): string => {
  return categories.find(c => c.id === categoryId)?.name || 'Unknown Category';
};

// Base spending data
const baseSpendingData = [
  { month: 'Jan', spending: 1200 },
  { month: 'Feb', spending: 1500 },
  { month: 'Mar', spending: 1300 },
  { month: 'Apr', spending: 1700 },
  { month: 'May', spending: 1600 },
  { month: 'Jun', spending: 1850 },
];

// Base spending categories data for Pie Chart
// Using category IDs for better linking
const baseSpendingCategoriesData = [
  { id: 'cat4', name: getCategoryName('cat4', SAMPLE_CATEGORIES), value: 450, fill: 'hsl(var(--chart-1))' }, // Supermercado
  { id: 'cat7', name: getCategoryName('cat7', SAMPLE_CATEGORIES), value: 220, fill: 'hsl(var(--chart-2))' }, // Bencina
  { id: 'cat12', name: getCategoryName('cat12', SAMPLE_CATEGORIES), value: 300, fill: 'hsl(var(--chart-3))'}, // Salir a Comer
  { id: 'cat16', name: getCategoryName('cat16', SAMPLE_CATEGORIES), value: 180, fill: 'hsl(var(--chart-4))' }, // Delivery
  { id: 'cat19', name: getCategoryName('cat19', SAMPLE_CATEGORIES), value: 150, fill: 'hsl(var(--chart-5))' }, // Streaming
];

const initialChartConfig = {
  spending: {
    label: 'Spending',
    color: 'hsl(var(--chart-1))',
  },
  ...baseSpendingCategoriesData.reduce((acc, item) => {
    acc[item.id] = { label: item.name, color: item.fill };
    return acc;
  }, {} as any)
};


export default function DashboardPage() {
  const [selectedBudgetClicksCategoryId, setSelectedBudgetClicksCategoryId] = useState<string | null>(null);
  const [selectedBudgetCategoryName, setSelectedBudgetCategoryName] = useState<string | null>(null);
  
  const [drilldownPieCategoryId, setDrilldownPieCategoryId] = useState<string | null>(null);
  const [drilldownPieCategoryName, setDrilldownPieCategoryName] = useState<string | null>(null);

  const totalActiveCategories = SAMPLE_CATEGORIES.length.toString();

  const budgetStatusData = useMemo(() => SAMPLE_BUDGETS.map(budget => {
    const category = SAMPLE_CATEGORIES.find(c => c.id === budget.categoryId);
    return {
      ...budget,
      name: category ? category.name : 'Unknown Category',
      value: budget.spent, // alias for spent for clarity in this context
    };
  }), []);

  const handleBudgetCategorySelect = (categoryId: string, categoryName: string) => {
    if (selectedBudgetClicksCategoryId === categoryId) {
      setSelectedBudgetClicksCategoryId(null); // Deselect if clicking the same category
      setSelectedBudgetCategoryName(null);
    } else {
      setSelectedBudgetClicksCategoryId(categoryId);
      setSelectedBudgetCategoryName(categoryName);
      setDrilldownPieCategoryId(null); // Reset pie drilldown
      setDrilldownPieCategoryName(null);
    }
  };
  
  const handlePieSliceClick = (data: any, event: MouseEvent) => {
    if (data && data.id) {
      if (drilldownPieCategoryId === data.id) {
        setDrilldownPieCategoryId(null); 
        setDrilldownPieCategoryName(null);
      } else {
        setDrilldownPieCategoryId(data.id);
        setDrilldownPieCategoryName(data.name);
        setSelectedBudgetClicksCategoryId(null); 
        setSelectedBudgetCategoryName(null);
      }
    }
  };

  const resetSelections = () => {
    setSelectedBudgetClicksCategoryId(null);
    setSelectedBudgetCategoryName(null);
    setDrilldownPieCategoryId(null);
    setDrilldownPieCategoryName(null);
  }

  const monthlySpendingTrendData = useMemo(() => {
    if (selectedBudgetClicksCategoryId) {
      const selectedBudgetInfo = budgetStatusData.find(b => b.categoryId === selectedBudgetClicksCategoryId);
      if (selectedBudgetInfo && selectedBudgetInfo.name.toLowerCase().includes('arriendo')) { 
        return baseSpendingData.map(d => ({ ...d, spending: selectedBudgetInfo.limit }));
      }
      return baseSpendingData.map(d => ({ ...d, spending: d.spending * (Math.random() * 0.4 + 0.3) })); 
    }
    return baseSpendingData;
  }, [selectedBudgetClicksCategoryId, budgetStatusData]);

  const merchantsForPieDrilldown = useMemo(() => {
    if (!drilldownPieCategoryId) return [];
    
    const categoryTransactions = SAMPLE_TRANSACTIONS.filter(tx => tx.categoryId === drilldownPieCategoryId && tx.status === 'categorized');
    
    const merchantTotals: { [key: string]: { name: string; amount: number; count: number } } = {};
    
    categoryTransactions.forEach(tx => {
      const merchantName = tx.description || 'Unknown Merchant';
      if (merchantTotals[merchantName]) {
        merchantTotals[merchantName].amount += tx.amount;
        merchantTotals[merchantName].count += 1;
      } else {
        merchantTotals[merchantName] = { name: merchantName, amount: tx.amount, count: 1 };
      }
    });
    
    return Object.values(merchantTotals).sort((a, b) => b.amount - a.amount);

  }, [drilldownPieCategoryId]);

  const projectionCardData = useMemo(() => {
    if (selectedBudgetClicksCategoryId && selectedBudgetCategoryName) {
      const budget = budgetStatusData.find(b => b.categoryId === selectedBudgetClicksCategoryId);
      const projected = budget ? budget.limit * 1.1 : 200; 
      return {
        title: `Proyección para ${selectedBudgetCategoryName}`,
        value: `$${projected.toFixed(2)}`,
        comparisonText: `Basado en el límite de $${budget?.limit.toFixed(2) || 'N/A'}.`
      };
    }
    return {
      title: "Proyección Fin de Mes",
      value: "$2,100.00",
      comparisonText: "Vas en camino a gastar un poco más que el mes pasado."
    };
  }, [selectedBudgetClicksCategoryId, selectedBudgetCategoryName, budgetStatusData]);

  const topSpendingCategoryOverall = useMemo(() => {
    if (baseSpendingCategoriesData.length === 0) return null;
    const topCategory = [...baseSpendingCategoriesData].sort((a, b) => b.value - a.value)[0];
    return {
        title: "Principal Categoría de Gasto (General)",
        value: topCategory.name,
        description: `Monto: $${topCategory.value.toFixed(2)}`
    };
  }, []);


  const barChartTitle = selectedBudgetClicksCategoryId && selectedBudgetCategoryName 
    ? `Tendencia de Gasto Mensual para ${selectedBudgetCategoryName}`
    : "Tendencia de Gasto Mensual";

  const chartConfig = useMemo(() => {
    return initialChartConfig;
  }, []);


  return (
    <div className="space-y-6">
      <PageHeader title="Resumen de Gastos" description="Tu resumen financiero de un vistazo.">
        {(selectedBudgetClicksCategoryId || drilldownPieCategoryId) && (
          <Button variant="outline" onClick={resetSelections} size="sm">
            <RefreshCw className="mr-2 h-4 w-4" /> Ver Resumen General
          </Button>
        )}
      </PageHeader>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <StatCard title="Gasto Total (Este Mes)" value="$1,850.75" icon={DollarSign} description="+20.1% desde el mes pasado" />
        <StatCard title="Presupuesto Restante" value="$675.25" icon={Target} description="Basado en presupuestos actuales" />
        <StatCard title="Gasto Proyectado" value={selectedBudgetClicksCategoryId && selectedBudgetCategoryName ? projectionCardData.value : "$2,100.00"} icon={TrendingUp} description={selectedBudgetClicksCategoryId && selectedBudgetCategoryName ? `Proyección para ${selectedBudgetCategoryName}` : "Basado en tendencias actuales"} />
        <StatCard title="Categorías Activas" value={totalActiveCategories} icon={Users} description={`Siguiendo ${totalActiveCategories} tipos de gastos`} />
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="font-headline">{barChartTitle}</CardTitle>
            <CardDescription>Comparación de gastos en los últimos 6 meses.</CardDescription>
          </CardHeader>
          <CardContent>
            <ChartContainer config={chartConfig} className="h-[300px] w-full">
              <BarChart accessibilityLayer data={monthlySpendingTrendData} margin={{ top: 5, right: 20, left: -10, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false}/>
                <XAxis dataKey="month" tickLine={false} axisLine={false} tickMargin={8} />
                <YAxis tickLine={false} axisLine={false} tickMargin={8} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <ChartLegend content={<ChartLegendContent />} />
                <Bar dataKey="spending" fill="var(--color-spending)" radius={4} />
              </BarChart>
            </ChartContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="font-headline">Estado del Presupuesto</CardTitle>
            <CardDescription>
              {selectedBudgetClicksCategoryId && selectedBudgetCategoryName 
                ? `Mostrando estado para ${selectedBudgetCategoryName}. Haz clic de nuevo para ver todos.`
                : "Resumen de tus asignaciones presupuestarias actuales. Haz clic en un presupuesto para filtrar."}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-1">
            {budgetStatusData.length > 0 ? budgetStatusData.map((budget) => (
              <div 
                key={budget.id} 
                onClick={() => handleBudgetCategorySelect(budget.categoryId, budget.name)}
                className={`p-2 rounded-md cursor-pointer hover:bg-accent/70 transition-colors ${selectedBudgetClicksCategoryId === budget.categoryId ? 'bg-accent ring-2 ring-primary' : 'hover:bg-muted/50'}`}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleBudgetCategorySelect(budget.categoryId, budget.name)}}
                aria-pressed={selectedBudgetClicksCategoryId === budget.categoryId}
              >
                <div className="mb-1 flex justify-between">
                  <span className="text-sm font-medium">{budget.name}</span>
                  <span className="text-sm text-muted-foreground">
                    ${budget.value.toFixed(2)} / ${budget.limit.toFixed(2)}
                  </span>
                </div>
                <Progress value={(budget.value / budget.limit) * 100} aria-label={`${budget.name} budget progress`} className="h-3" />
              </div>
            )) : (
              <p className="text-muted-foreground">Aún no hay presupuestos definidos. Ve a la página de Presupuesto para añadir algunos.</p>
            )}
          </CardContent>
        </Card>
      </div>
      
      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <div>
                <CardTitle className="font-headline">
                  {drilldownPieCategoryId && drilldownPieCategoryName ? `Desglose de ${drilldownPieCategoryName}` : "Gasto por Categoría"}
                </CardTitle>
                <CardDescription>
                  {drilldownPieCategoryId ? "Comercios/descripciones con más gastos." : "Desglose de gastos por categoría este mes. Haz clic en una porción para ver detalles."}
                </CardDescription>
              </div>
              {drilldownPieCategoryId && (
                <Button variant="outline" size="sm" onClick={() => { setDrilldownPieCategoryId(null); setDrilldownPieCategoryName(null); }}>
                  <ArrowLeft className="mr-2 h-4 w-4" /> Volver al Gráfico
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent className="flex justify-center">
            {drilldownPieCategoryId && drilldownPieCategoryName ? (
                merchantsForPieDrilldown.length > 0 ? (
                <div className="w-full">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Comercio/Descripción</TableHead>
                        <TableHead className="text-right">Monto</TableHead>
                        <TableHead className="text-right">Transacciones</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {merchantsForPieDrilldown.map(merchant => (
                        <TableRow key={merchant.name}>
                          <TableCell className="font-medium">{merchant.name}</TableCell>
                          <TableCell className="text-right">${merchant.amount.toFixed(2)}</TableCell>
                          <TableCell className="text-right">{merchant.count}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              ) : (
                <p className="text-muted-foreground">No hay transacciones categorizadas para mostrar detalles de {drilldownPieCategoryName}.</p>
              )
            ) : (
              <ChartContainer config={chartConfig} className="h-[300px] w-full max-w-xs">
                <PieChart accessibilityLayer >
                  <ChartTooltip content={<ChartTooltipContent hideLabel />} />
                  <Pie 
                    data={baseSpendingCategoriesData} 
                    dataKey="value" 
                    nameKey="name" 
                    cx="50%" 
                    cy="50%" 
                    outerRadius={100} 
                    labelLine={false} 
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                    onClick={handlePieSliceClick}
                    className="cursor-pointer"
                  >
                    {baseSpendingCategoriesData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.fill} className="focus:outline-none focus:ring-2 focus:ring-ring rounded-full" tabIndex={0} />
                    ))}
                  </Pie>
                  <ChartLegend content={<ChartLegendContent nameKey="name" />} />
                </PieChart>
              </ChartContainer>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="font-headline">Proyecciones y Perspectivas</CardTitle>
            <CardDescription>
                {selectedBudgetClicksCategoryId && selectedBudgetCategoryName 
                ? `Perspectivas para ${selectedBudgetCategoryName}.`
                : "Perspectiva futura basada en tus hábitos de gasto."}
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
             <div className="flex items-start space-x-4 p-4 bg-accent/20 rounded-lg">
                <TrendingUp className="h-7 w-7 text-primary mt-1 flex-shrink-0" />
                <div>
                    <p className="text-sm font-semibold">{projectionCardData.title}</p>
                    <p className="text-xl font-bold text-primary">{projectionCardData.value}</p>
                    <p className="text-xs text-muted-foreground">{projectionCardData.comparisonText}</p>
                </div>
            </div>
             <div className="flex items-start space-x-4 p-4 bg-secondary/30 rounded-lg">
                <BarChartHorizontalBig className="h-7 w-7 text-green-600 mt-1 flex-shrink-0" />
                <div>
                    <p className="text-sm font-semibold">Comparación: Mes Anterior</p>
                    <p className="text-xl font-bold text-green-600">-$150.23</p>
                    <p className="text-xs text-muted-foreground">Gastaste menos este mes en comparación con el mismo período del mes pasado.</p>
                </div>
            </div>
            {topSpendingCategoryOverall && (
                <div className="flex items-start space-x-4 p-4 bg-accent/20 rounded-lg">
                    <ListTree className="h-7 w-7 text-primary/80 mt-1 flex-shrink-0" />
                    <div>
                        <p className="text-sm font-semibold">{topSpendingCategoryOverall.title}</p>
                        <p className="text-lg font-bold text-primary/90">{topSpendingCategoryOverall.value}</p>
                        <p className="text-xs text-muted-foreground">{topSpendingCategoryOverall.description}</p>
                    </div>
                </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
