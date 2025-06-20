'use client';

import { useState } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { PiggyBank, Edit, Trash2, PlusCircle, Save } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogClose,
} from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { SAMPLE_BUDGETS, SAMPLE_CATEGORIES, type Budget, type Category } from '@/lib/constants';
import { useToast } from '@/hooks/use-toast';

export default function BudgetPage() {
  const [budgets, setBudgets] = useState<Budget[]>(SAMPLE_BUDGETS);
  const [categories] = useState<Category[]>(SAMPLE_CATEGORIES);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<Partial<Budget> & { categoryName?: string } | null>(null);
  const [newBudgetAmount, setNewBudgetAmount] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const { toast } = useToast();

  const getCategoryName = (categoryId: string) => categories.find(c => c.id === categoryId)?.name || 'Unknown Category';

  const handleAddOrEditBudget = () => {
    if (!selectedCategoryId || !newBudgetAmount) {
      toast({ title: "Error", description: "Please select a category and enter an amount.", variant: "destructive" });
      return;
    }
    const amount = parseFloat(newBudgetAmount);
    if (isNaN(amount) || amount <= 0) {
      toast({ title: "Error", description: "Please enter a valid positive amount.", variant: "destructive" });
      return;
    }

    if (editingBudget && editingBudget.id) {
      // Edit existing budget
      setBudgets(prevBudgets =>
        prevBudgets.map(b =>
          b.id === editingBudget.id ? { ...b, categoryId: selectedCategoryId, limit: amount } : b
        )
      );
      toast({ title: "Success", description: `Budget for ${getCategoryName(selectedCategoryId)} updated.` });
    } else {
      // Add new budget
      // Check if budget for this category already exists
      if (budgets.some(b => b.categoryId === selectedCategoryId)) {
        toast({ title: "Error", description: `Budget for ${getCategoryName(selectedCategoryId)} already exists. Please edit the existing one.`, variant: "destructive" });
        return;
      }
      const newBudgetEntry: Budget = {
        id: `bud${Date.now()}`,
        categoryId: selectedCategoryId,
        limit: amount,
        spent: 0, // Assuming new budgets start with 0 spent
      };
      setBudgets(prevBudgets => [...prevBudgets, newBudgetEntry]);
      toast({ title: "Success", description: `Budget for ${getCategoryName(selectedCategoryId)} added.` });
    }
    closeDialog();
  };

  const openEditDialog = (budget: Budget) => {
    setEditingBudget({ ...budget, categoryName: getCategoryName(budget.categoryId) });
    setSelectedCategoryId(budget.categoryId);
    setNewBudgetAmount(budget.limit.toString());
    setIsDialogOpen(true);
  };
  
  const openAddDialog = () => {
    setEditingBudget(null);
    setSelectedCategoryId('');
    setNewBudgetAmount('');
    setIsDialogOpen(true);
  };

  const closeDialog = () => {
    setIsDialogOpen(false);
    setEditingBudget(null);
    setSelectedCategoryId('');
    setNewBudgetAmount('');
  };

  const handleDeleteBudget = (budgetId: string) => {
    const budgetToDelete = budgets.find(b => b.id === budgetId);
    if (budgetToDelete) {
        setBudgets(prevBudgets => prevBudgets.filter(b => b.id !== budgetId));
        toast({ title: "Success", description: `Budget for ${getCategoryName(budgetToDelete.categoryId)} deleted.` });
    }
  };


  return (
    <div className="space-y-6">
      <PageHeader title="Budget Management" description="Define and adjust your spending limits.">
        <Button onClick={openAddDialog}>
          <PlusCircle className="mr-2 h-4 w-4" /> Add New Budget
        </Button>
      </PageHeader>

      <Card className="shadow-lg">
        <CardHeader>
          <CardTitle className="font-headline flex items-center gap-2">
            <PiggyBank className="h-6 w-6 text-primary" />
            Your Budgets
          </CardTitle>
          <CardDescription>
            Track your spending against your set limits for each category.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Category</TableHead>
                <TableHead className="text-right">Limit</TableHead>
                <TableHead className="text-right">Spent</TableHead>
                <TableHead className="text-right">Remaining</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {budgets.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    No budgets defined yet. Click "Add New Budget" to start.
                  </TableCell>
                </TableRow>
              )}
              {budgets.map((budget) => {
                const remaining = budget.limit - budget.spent;
                const progress = (budget.spent / budget.limit) * 100;
                const category = categories.find(c => c.id === budget.categoryId);
                return (
                  <TableRow key={budget.id}>
                    <TableCell className="font-medium flex items-center gap-2">
                      {category?.icon && <category.icon className="h-5 w-5 text-muted-foreground" />}
                      {getCategoryName(budget.categoryId)}
                    </TableCell>
                    <TableCell className="text-right">${budget.limit.toFixed(2)}</TableCell>
                    <TableCell className="text-right">${budget.spent.toFixed(2)}</TableCell>
                    <TableCell className={`text-right ${remaining < 0 ? 'text-destructive' : 'text-green-600'}`}>
                      ${remaining.toFixed(2)}
                    </TableCell>
                    <TableCell>
                      <Progress value={progress > 100 ? 100 : progress} aria-label={`${getCategoryName(budget.categoryId)} budget progress`} className="h-3 w-32" />
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEditDialog(budget)} aria-label="Edit budget">
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => handleDeleteBudget(budget.id)} className="text-destructive hover:text-destructive" aria-label="Delete budget">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle className="font-headline">{editingBudget ? 'Edit Budget' : 'Add New Budget'}</DialogTitle>
            <DialogDescription>
              {editingBudget ? `Modify the budget for ${editingBudget.categoryName}.` : 'Set a spending limit for a new category.'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="category" className="text-right">
                Category
              </Label>
              <Select 
                value={selectedCategoryId} 
                onValueChange={setSelectedCategoryId}
                disabled={!!editingBudget} // Disable if editing, category shouldn't change
              >
                <SelectTrigger className="col-span-3" id="category" aria-label="Select category">
                  <SelectValue placeholder="Select a category" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat.id} value={cat.id} disabled={!editingBudget && budgets.some(b => b.categoryId === cat.id)}>
                      {cat.name} {!editingBudget && budgets.some(b => b.categoryId === cat.id) && "(Budgeted)"}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="amount" className="text-right">
                Amount ($)
              </Label>
              <Input
                id="amount"
                type="number"
                value={newBudgetAmount}
                onChange={(e) => setNewBudgetAmount(e.target.value)}
                className="col-span-3"
                placeholder="e.g., 500"
              />
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
                <Button type="button" variant="outline" onClick={closeDialog}>Cancel</Button>
            </DialogClose>
            <Button type="submit" onClick={handleAddOrEditBudget}>
              <Save className="mr-2 h-4 w-4" /> {editingBudget ? 'Save Changes' : 'Add Budget'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
