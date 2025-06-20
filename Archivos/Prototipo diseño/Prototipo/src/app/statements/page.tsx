
'use client';

import { useState, type ChangeEvent, useEffect, useRef } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { UploadCloud, FileText, AlertTriangle, CheckCircle2, CalendarDays, FileType2 } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';
import { Progress } from '@/components/ui/progress';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { SAMPLE_CATEGORIES, type Category, type Transaction, type CardType } from '@/lib/constants';
import { Skeleton } from '@/components/ui/skeleton';

interface UploadedFile {
  id: string;
  name: string;
  size: number;
  status: 'uploading' | 'processing' | 'success' | 'error';
  message?: string;
  progress?: number;
  fileType?: FileUploadType;
  month?: string;
  year?: string;
  processedTransactionsCount?: number;
}

type FileUploadType = 'estado_cierre' | 'ultimos_movimientos';

const months = Array.from({ length: 12 }, (_, i) => ({
  value: (i + 1).toString(),
  label: new Date(0, i).toLocaleString('default', { month: 'long' }),
}));

const getCategoryIdByName = (name: string, categories: Category[]): string | undefined => {
  const foundCategory = categories.find(cat => cat.name.toLowerCase() === name.toLowerCase());
  return foundCategory?.id;
};

export default function StatementsPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const { toast } = useToast();

  const [fileType, setFileType] = useState<FileUploadType | undefined>(undefined);
  const [selectedMonth, setSelectedMonth] = useState<string>('');
  const [selectedYear, setSelectedYear] = useState<string>('');
  const [dynamicYears, setDynamicYears] = useState<{value: string, label: string}[]>([]);
  
  const [allTransactions, setAllTransactions] = useState<Transaction[]>([]); 
  const [isClient, setIsClient] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setIsClient(true); 
    const now = new Date();
    setSelectedMonth((now.getMonth() + 1).toString());
    const currentYearVal = now.getFullYear();
    setSelectedYear(currentYearVal.toString());
    setDynamicYears(
      Array.from({ length: 10 }, (_, i) => ({
        value: (currentYearVal - 5 + i).toString(),
        label: (currentYearVal - 5 + i).toString(),
      }))
    );
  }, []);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      toast({ title: 'No file selected', description: 'Please select a statement file to upload.', variant: 'destructive' });
      return;
    }
    if (!fileType) {
      toast({ title: 'No file type selected', description: 'Please select the type of file.', variant: 'destructive' });
      return;
    }
    if (!selectedMonth || !selectedYear) {
      toast({ title: 'Month/Year not selected', description: 'Please select the month and year for the statement.', variant: 'destructive' });
      return;
    }

    const newFileEntry: UploadedFile = {
      id: `file-${Date.now()}-${selectedFile.name}`,
      name: selectedFile.name,
      size: selectedFile.size,
      status: 'uploading',
      progress: 0,
      fileType: fileType,
      month: selectedMonth,
      year: selectedYear,
    };
    setUploadedFiles(prev => [newFileEntry, ...prev.slice(0, 4)]);

    await new Promise(resolve => setTimeout(resolve, 500));
    setUploadedFiles(prev => prev.map(f => f.id === newFileEntry.id ? {...f, progress: 30} : f));
    
    // SIMULATION: Actual XLS parsing would require a library like SheetJS.
    // This simulation will just create some dummy transactions based on file type.
    let simulatedTransactions: Transaction[] = [];
    const period = `${selectedYear}-${selectedMonth.padStart(2, '0')}`;

    toast({ title: "Simulating File Processing", description: "Actual XLS parsing is not implemented in this prototype."});

    if (fileType === 'estado_cierre') {
        simulatedTransactions = [
            { id: `ec1-${Date.now()}`, date: `${period}-01`, description: 'Pago Arriendo (Sim-XLS)', amount: 500000, cardType: 'titular', categoryId: getCategoryIdByName('Arriendo', SAMPLE_CATEGORIES), status: 'categorized' },
            { id: `ec2-${Date.now()}`, date: `${period}-05`, description: 'Supermercado Lider (Sim-XLS)', amount: 85000, cardType: 'titular', categoryId: getCategoryIdByName('Supermercado', SAMPLE_CATEGORIES), status: 'categorized' },
            { id: `ec3-${Date.now()}`, date: `${period}-10`, description: 'Cuentas Luz (Sim-XLS)', amount: 25000, cardType: 'familia', categoryId: getCategoryIdByName('Luz', SAMPLE_CATEGORIES), status: 'categorized' },
        ];
        setAllTransactions(prev => [
            ...prev.filter(tx => !tx.date.startsWith(period)), 
            ...simulatedTransactions 
        ]);
    } else if (fileType === 'ultimos_movimientos') {
        simulatedTransactions = [
            { id: `um1-${Date.now()}`, date: `${period}-15`, description: 'Compra en linea Amazon (Sim-XLS)', amount: 35000, cardType: 'titular', status: 'uncategorized' },
            { id: `um2-${Date.now()}`, date: `${period}-16`, description: 'Restaurante "El Sabor" (Sim-XLS)', amount: 22000, cardType: 'familia', status: 'uncategorized' },
        ];
        const newUniqueTransactions = simulatedTransactions.filter(newTx => 
            !allTransactions.some(existingTx => 
                existingTx.date.startsWith(period) &&
                existingTx.description === newTx.description && 
                existingTx.amount === newTx.amount
            )
        );
        setAllTransactions(prev => [...prev, ...newUniqueTransactions]);
    }
    
    await new Promise(resolve => setTimeout(resolve, 1000));
    setUploadedFiles(prev => prev.map(f => f.id === newFileEntry.id ? {...f, progress: 70, status: 'processing'} : f));
    await new Promise(resolve => setTimeout(resolve, 1500));

    const success = simulatedTransactions.length > 0 || (fileType === 'estado_cierre'); 
    
    if (success) {
      setUploadedFiles(prev => prev.map(f => f.id === newFileEntry.id ? {...f, progress: 100, status: 'success', message: `${fileType === 'estado_cierre' ? 'Closing statement' : 'Recent movements'} (XLS simulated) processed. ${simulatedTransactions.length} transactions found/updated.`, processedTransactionsCount: simulatedTransactions.length } : f));
      const currentMonthLabel = months.find(m=>m.value===selectedMonth)?.label || selectedMonth;
      toast({
        title: 'Statement Processed (Simulated)!',
        description: `File ${selectedFile.name} (${fileType}) for ${currentMonthLabel} ${selectedYear} processed. ${simulatedTransactions.length} transactions.`,
      });
    } else {
      setUploadedFiles(prev => prev.map(f => f.id === newFileEntry.id ? {...f, progress: 100, status: 'error', message: 'Failed to simulate parse or no new data found.'} : f));
      toast({
        title: 'Upload Issue (Simulated)',
        description: `Could not process ${selectedFile.name}.`,
        variant: 'destructive',
      });
    }

    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };
  
  useEffect(() => {
    if (allTransactions.length > 0) {
      // console.log(`Total transactions in memory: ${allTransactions.length}`);
    }
  }, [allTransactions]);

  return (
    <div className="space-y-6">
      <PageHeader title="Upload Statements" description="Upload your bank statements (XLS files) to update transactions." />

      <Card className="shadow-lg">
        <CardHeader>
          <CardTitle className="font-headline flex items-center gap-2">
            <UploadCloud className="h-6 w-6 text-primary" />
            Upload New Statement
          </CardTitle>
          <CardDescription>
            Select the file type, period, and then upload your statement (XLS or XLSX format).
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {isClient ? (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
              <div>
                <Label htmlFor="fileType" className="flex items-center gap-1 mb-1">
                  <FileType2 className="h-4 w-4" /> File Type
                </Label>
                <Select value={fileType} onValueChange={(value) => setFileType(value as FileUploadType)}>
                  <SelectTrigger id="fileType" aria-label="Select file type">
                    <SelectValue placeholder="Select file type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="estado_cierre">Estado de Cierre (Monthly)</SelectItem>
                    <SelectItem value="ultimos_movimientos">Últimos Movimientos (Current Month)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="month" className="flex items-center gap-1 mb-1">
                  <CalendarDays className="h-4 w-4" /> Month
                </Label>
                <Select value={selectedMonth} onValueChange={setSelectedMonth} disabled={!selectedMonth}>
                  <SelectTrigger id="month" aria-label="Select month">
                    <SelectValue placeholder="Select month" />
                  </SelectTrigger>
                  <SelectContent>
                    {months.map(m => <SelectItem key={m.value} value={m.value}>{m.label}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="year" className="flex items-center gap-1 mb-1">
                  <CalendarDays className="h-4 w-4" /> Year
                </Label>
                 <Select value={selectedYear} onValueChange={setSelectedYear} disabled={dynamicYears.length === 0 || !selectedYear}>
                  <SelectTrigger id="year" aria-label="Select year">
                    <SelectValue placeholder="Select year" />
                  </SelectTrigger>
                  <SelectContent>
                    {dynamicYears.map(y => <SelectItem key={y.value} value={y.value}>{y.label}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
                <Skeleton className="h-10 w-full rounded-md" />
                <Skeleton className="h-10 w-full rounded-md" />
                <Skeleton className="h-10 w-full rounded-md" />
            </div>
          )}

          <div className="flex flex-col items-center justify-center space-y-3 rounded-lg border-2 border-dashed border-border p-8 text-center hover:border-primary transition-colors">
            <UploadCloud className="h-12 w-12 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              Drag & drop your Excel file (.xls, .xlsx) here, or click to browse.
            </p>
            <Input 
              id="statement-upload-input" 
              type="file" 
              ref={fileInputRef} 
              onChange={handleFileChange} 
              className="sr-only" 
              aria-label="Statement file uploader" 
              accept=".xls,.xlsx,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            />
             <Button variant="outline" onClick={() => fileInputRef.current?.click()}>
                Browse Files
              </Button>
            {selectedFile && <p className="text-sm text-foreground">Selected: {selectedFile.name}</p>}
          </div>
        </CardContent>
        <CardFooter>
          <Button 
            onClick={handleUpload} 
            disabled={!selectedFile || !fileType || !selectedMonth || !selectedYear || !isClient} 
            className="w-full md:w-auto"
          >
            <UploadCloud className="mr-2 h-4 w-4" />
            Upload and Process (Simulated)
          </Button>
        </CardFooter>
      </Card>

      {uploadedFiles.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="font-headline">Upload History (Last 5)</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {uploadedFiles.map((file) => (
              <div key={file.id} className="rounded-lg border p-4">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex items-center gap-3">
                    {file.status === 'success' && <CheckCircle2 className="h-6 w-6 text-green-500 flex-shrink-0" />}
                    {file.status === 'error' && <AlertTriangle className="h-6 w-6 text-red-500 flex-shrink-0" />}
                    {(file.status === 'uploading' || file.status === 'processing') && <FileText className="h-6 w-6 text-primary flex-shrink-0" />}
                    <div>
                      <p className="font-medium">{file.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {(file.size / 1024).toFixed(2)} KB | 
                        Type: {file.fileType?.replace('_', ' ')} | 
                        Period: {months.find(m=>m.value===file.month)?.label || file.month} {file.year}
                      </p>
                    </div>
                  </div>
                  <div className="text-sm text-right">
                    {file.status === 'uploading' && `Uploading... ${file.progress}%`}
                    {file.status === 'processing' && `Processing... ${file.progress}%`}
                    {file.status === 'success' && <span className="text-green-500">Success ({file.processedTransactionsCount} txns)</span>}
                    {file.status === 'error' && <span className="text-red-500">Error</span>}
                  </div>
                </div>
                {(file.status === 'uploading' || file.status === 'processing') && file.progress !== undefined && (
                  <Progress value={file.progress} className="mt-2 h-2" />
                )}
                {file.message && <p className={`mt-2 text-xs ${file.status === 'error' ? 'text-red-500' : 'text-muted-foreground'}`}>{file.message}</p>}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {allTransactions.length > 0 && isClient && ( // Added isClient check for this section too
        <Card className="mt-6">
            <CardHeader>
                <CardTitle className="font-headline">Currently Loaded Transactions (Last 5)</CardTitle>
                <CardDescription>This shows a sample of transactions currently in application memory. Total: {allTransactions.length}</CardDescription>
            </CardHeader>
            <CardContent>
                <ul className="space-y-2">
                    {allTransactions.slice(-5).reverse().map(tx => (
                        <li key={tx.id} className="text-sm p-2 border rounded-md">
                            {tx.date} - {tx.description} - ${tx.amount.toFixed(2)} - Cat: {SAMPLE_CATEGORIES.find(c=>c.id === tx.categoryId)?.name || tx.status}
                        </li>
                    ))}
                </ul>
            </CardContent>
        </Card>
      )}
    </div>
  );
}

