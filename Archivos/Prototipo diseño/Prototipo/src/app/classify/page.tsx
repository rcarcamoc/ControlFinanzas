'use client';

import { useState, useEffect } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ListChecks, Tag, CreditCard, Check, HelpCircle } from 'lucide-react';
import { SAMPLE_TRANSACTIONS, SAMPLE_CATEGORIES, CARD_TYPES, type Transaction, type Category, type CardType } from '@/lib/constants';
import { Badge } from '@/components/ui/badge';
import { useToast } from '@/hooks/use-toast';

// Simulate fetching previously used categories for a transaction description
const getSuggestedCategories = (description: string): string[] => {
  // In a real app, this would query a database or local storage
  if (description.toLowerCase().includes('supermart')) return ['cat1']; // Groceries
  if (description.toLowerCase().includes('cafe')) return ['cat4']; // Dining Out
  return [];
};

export default function ClassifyPage() {
  const [transactions, setTransactions] = useState<Transaction[]>(SAMPLE_TRANSACTIONS);
  const [categories] = useState<Category[]>(SAMPLE_CATEGORIES);
  const { toast } = useToast();

  // State to hold individual selections for each transaction
  const [selectedCardTypes, setSelectedCardTypes] = useState<{ [key: string]: CardType | undefined }>({});
  const [selectedCategories, setSelectedCategories] = useState<{ [key: string]: string | undefined }>({});

  // Initialize selections based on existing categorized transactions
  useEffect(() => {
    const initialCardTypes: { [key: string]: CardType | undefined } = {};
    const initialCategories: { [key: string]: string | undefined } = {};
    transactions.forEach(tx => {
      if (tx.status === 'categorized') {
        initialCardTypes[tx.id] = tx.cardType;
        initialCategories[tx.id] = tx.categoryId;
      }
    });
    setSelectedCardTypes(initialCardTypes);
    setSelectedCategories(initialCategories);
  }, []); // Run once on mount based on initial transactions

  const handleCardTypeChange = (transactionId: string, value: CardType) => {
    setSelectedCardTypes(prev => ({ ...prev, [transactionId]: value }));
  };

  const handleCategoryChange = (transactionId: string, value: string) => {
    setSelectedCategories(prev => ({ ...prev, [transactionId]: value }));
  };

  const handleSaveClassification = (transactionId: string) => {
    const cardType = selectedCardTypes[transactionId];
    const categoryId = selectedCategories[transactionId];

    if (!cardType || !categoryId) {
      toast({ title: "Incomplete", description: "Please select both card type and category.", variant: "destructive" });
      return;
    }

    setTransactions(prevTxns =>
      prevTxns.map(tx =>
        tx.id === transactionId ? { ...tx, cardType, categoryId, status: 'categorized' } : tx
      )
    );
    toast({ title: "Saved!", description: `Transaction classified successfully.` });
  };
  
  const uncategorizedTransactions = transactions.filter(tx => tx.status === 'uncategorized');
  const categorizedTransactions = transactions.filter(tx => tx.status === 'categorized');

  const renderTransactionRow = (tx: Transaction, isUncategorized: boolean) => {
    const suggestedCategoryIds = isUncategorized ? getSuggestedCategories(tx.description) : [];
    const currentCardType = selectedCardTypes[tx.id] || tx.cardType;
    const currentCategoryId = selectedCategories[tx.id] || tx.categoryId;
    
    return (
      <TableRow key={tx.id} className={isUncategorized ? "bg-accent/20" : ""}>
        <TableCell>{new Date(tx.date).toLocaleDateString()}</TableCell>
        <TableCell className="font-medium">{tx.description}</TableCell>
        <TableCell className="text-right">${tx.amount.toFixed(2)}</TableCell>
        <TableCell>
          <Select
            value={currentCardType}
            onValueChange={(value) => handleCardTypeChange(tx.id, value as CardType)}
            disabled={!isUncategorized && tx.status === 'categorized'}
          >
            <SelectTrigger className="w-[120px]" aria-label={`Card type for ${tx.description}`}>
              <SelectValue placeholder="Card Type" />
            </SelectTrigger>
            <SelectContent>
              {CARD_TYPES.map(ct => (
                <SelectItem key={ct.value} value={ct.value}>{ct.label}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </TableCell>
        <TableCell>
          <Select
            value={currentCategoryId}
            onValueChange={(value) => handleCategoryChange(tx.id, value)}
            disabled={!isUncategorized && tx.status === 'categorized'}
          >
            <SelectTrigger className="w-[180px]" aria-label={`Category for ${tx.description}`}>
              <SelectValue placeholder="Select Category" />
            </SelectTrigger>
            <SelectContent>
              {suggestedCategoryIds.length > 0 && (
                <>
                  <p className="p-2 text-xs text-muted-foreground">Suggestions:</p>
                  {suggestedCategoryIds.map(catId => {
                    const category = categories.find(c => c.id === catId);
                    return category ? <SelectItem key={`sug-${catId}`} value={catId}>{category.name}</SelectItem> : null;
                  })}
                </>
              )}
              <p className="p-2 text-xs text-muted-foreground">{suggestedCategoryIds.length > 0 ? 'All Categories:' : 'Categories:'}</p>
              {categories.map(cat => (
                <SelectItem key={cat.id} value={cat.id}>{cat.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </TableCell>
        <TableCell className="text-right">
          {isUncategorized ? (
            <Button size="sm" onClick={() => handleSaveClassification(tx.id)} disabled={!selectedCardTypes[tx.id] || !selectedCategories[tx.id]}>
              <Check className="mr-2 h-4 w-4" /> Save
            </Button>
          ) : (
            <Badge variant="default" className="bg-green-500 hover:bg-green-600">Categorized</Badge>
          )}
        </TableCell>
      </TableRow>
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Classify Transactions" description="Categorize your new and existing transactions." />

      {uncategorizedTransactions.length > 0 && (
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="font-headline flex items-center gap-2">
              <HelpCircle className="h-6 w-6 text-destructive" />
              Uncategorized Transactions
            </CardTitle>
            <CardDescription>
              These transactions need your attention. Please assign a card type and category.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead><CreditCard className="inline h-4 w-4 mr-1" />Card Type</TableHead>
                  <TableHead><Tag className="inline h-4 w-4 mr-1" />Category</TableHead>
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {uncategorizedTransactions.map(tx => renderTransactionRow(tx, true))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
      
      {uncategorizedTransactions.length === 0 && transactions.length > 0 && (
         <Card className="shadow-lg">
            <CardHeader>
                <CardTitle className="font-headline flex items-center gap-2">
                <Check className="h-6 w-6 text-green-500" />
                All Caught Up!
                </CardTitle>
            </CardHeader>
            <CardContent>
                <p className="text-muted-foreground">There are no transactions currently needing categorization.</p>
            </CardContent>
        </Card>
      )}

      {categorizedTransactions.length > 0 && (
        <Card className="shadow-lg mt-6">
          <CardHeader>
            <CardTitle className="font-headline flex items-center gap-2">
              <ListChecks className="h-6 w-6 text-primary" />
              Categorized Transactions
            </CardTitle>
            <CardDescription>
              Review or re-classify your past transactions if needed. (Re-classification not yet implemented)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Amount</TableHead>
                  <TableHead>Card Type</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {categorizedTransactions.map(tx => renderTransactionRow(tx, false))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

       {transactions.length === 0 && (
         <Card className="shadow-lg">
            <CardHeader>
                <CardTitle className="font-headline">No Transactions</CardTitle>
            </CardHeader>
            <CardContent>
                <p className="text-muted-foreground">Upload a statement to get started.</p>
            </CardContent>
        </Card>
      )}

    </div>
  );
}
