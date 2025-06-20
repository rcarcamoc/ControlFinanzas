
'use client';

import { useState } from 'react';
import { PageHeader } from '@/components/shared/PageHeader';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { 
  Tags, Edit, Trash2, PlusCircle, Save, // UI elements
  // Icons for categories
  ShoppingCart, Home, Car, Utensils, HeartPulse, BookOpen, // Some existing/general
  CreditCard, Plane, FileText, PawPrint, Fuel, Carrot, Cat, Shield, Store, Flame, CircleDollarSign, Package, Lightbulb, Wifi, PlaySquare, Heart, Droplet, Pill, Wrench, Stethoscope, Gift, Landmark, Cookie, AlertTriangle, // New additions
  type LucideIcon // Type
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from '@/components/ui/dialog';
import { SAMPLE_CATEGORIES, type Category } from '@/lib/constants';
import { useToast } from '@/hooks/use-toast';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const ICONS_MAP: { [key: string]: LucideIcon } = {
  // All icons from the new SAMPLE_CATEGORIES
  Home, CreditCard, Plane, ShoppingCart, FileText, PawPrint, Fuel, Carrot, Cat, Car, Shield, Utensils, Store, Flame, CircleDollarSign, Package, Lightbulb, Wifi, PlaySquare, Heart, Droplet, Pill, Wrench, Stethoscope, Gift, Landmark, Cookie, AlertTriangle,
  // Keep existing ones from the old map for broader selection or if user customizes
  Tags, HeartPulse, BookOpen 
};
const ICON_NAMES = Object.keys(ICONS_MAP);

const getIconComponent = (iconName?: string): LucideIcon | undefined => {
  if (iconName && ICONS_MAP[iconName]) {
    return ICONS_MAP[iconName];
  }
  return Tags; // Default to Tags if not found or not selected
};


export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>(SAMPLE_CATEGORIES);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [selectedIconName, setSelectedIconName] = useState<string>('');
  const { toast } = useToast();

  const handleAddOrEditCategory = () => {
    if (!newCategoryName.trim()) {
      toast({ title: "Error", description: "Category name cannot be empty.", variant: "destructive" });
      return;
    }

    const iconComponent = getIconComponent(selectedIconName) || Tags; // Ensure a fallback

    if (editingCategory) {
      // Edit existing category
      setCategories(prevCategories =>
        prevCategories.map(c =>
          c.id === editingCategory.id ? { ...c, name: newCategoryName.trim(), icon: iconComponent } : c
        )
      );
      toast({ title: "Success", description: `Category "${newCategoryName.trim()}" updated.` });
    } else {
      // Add new category
      const newCategoryEntry: Category = {
        id: `cat${Date.now()}`, // Ensure unique ID
        name: newCategoryName.trim(),
        icon: iconComponent,
      };
      setCategories(prevCategories => [...prevCategories, newCategoryEntry]);
      toast({ title: "Success", description: `Category "${newCategoryName.trim()}" added.` });
    }
    closeDialog();
  };

  const openEditDialog = (category: Category) => {
    setEditingCategory(category);
    setNewCategoryName(category.name);
    const iconName = Object.keys(ICONS_MAP).find(name => ICONS_MAP[name] === category.icon) || '';
    setSelectedIconName(iconName);
    setIsDialogOpen(true);
  };

  const openAddDialog = () => {
    setEditingCategory(null);
    setNewCategoryName('');
    setSelectedIconName(''); // Default to no icon selected or a placeholder string if your Select needs it
    setIsDialogOpen(true);
  };

  const closeDialog = () => {
    setIsDialogOpen(false);
    setEditingCategory(null);
    setNewCategoryName('');
    setSelectedIconName('');
  };
  
  const handleDeleteCategory = (categoryId: string) => {
    const categoryToDelete = categories.find(c => c.id === categoryId);
     if (categoryToDelete) {
        // Basic check: Ideally, also check if category is used in budgets/transactions before allowing deletion.
        // For now, just remove it.
        setCategories(prevCategories => prevCategories.filter(c => c.id !== categoryId));
        toast({ title: "Success", description: `Category "${categoryToDelete.name}" deleted.` });
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Category Management" description="Add, edit, or remove expense categories.">
        <Button onClick={openAddDialog}>
          <PlusCircle className="mr-2 h-4 w-4" /> Add New Category
        </Button>
      </PageHeader>

      <Card className="shadow-lg">
        <CardHeader>
          <CardTitle className="font-headline flex items-center gap-2">
            <Tags className="h-6 w-6 text-primary" />
            Your Categories
          </CardTitle>
          <CardDescription>
            Manage the categories used for classifying your expenses and setting budgets.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Icon</TableHead>
                <TableHead>Name</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {categories.length === 0 && (
                <TableRow>
                  <TableCell colSpan={3} className="text-center text-muted-foreground">
                    No categories defined yet. Click "Add New Category" to start.
                  </TableCell>
                </TableRow>
              )}
              {categories.map((category) => {
                const IconComponent = category.icon || Tags; // Fallback to Tags icon
                return (
                  <TableRow key={category.id}>
                    <TableCell>
                      <IconComponent className="h-5 w-5 text-muted-foreground" />
                    </TableCell>
                    <TableCell className="font-medium">{category.name}</TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="icon" onClick={() => openEditDialog(category)} aria-label="Edit category">
                        <Edit className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => handleDeleteCategory(category.id)} className="text-destructive hover:text-destructive" aria-label="Delete category">
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
            <DialogTitle className="font-headline">{editingCategory ? 'Edit Category' : 'Add New Category'}</DialogTitle>
            <DialogDescription>
              {editingCategory ? `Modify the category "${editingCategory.name}".` : 'Create a new expense category.'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="name" className="text-right">
                Name
              </Label>
              <Input
                id="name"
                value={newCategoryName}
                onChange={(e) => setNewCategoryName(e.target.value)}
                className="col-span-3"
                placeholder="e.g., Groceries"
              />
            </div>
            <div className="grid grid-cols-4 items-center gap-4">
              <Label htmlFor="icon" className="text-right">
                Icon
              </Label>
              <Select value={selectedIconName} onValueChange={setSelectedIconName}>
                <SelectTrigger className="col-span-3" id="icon-select">
                  <SelectValue placeholder="Select an icon" />
                </SelectTrigger>
                <SelectContent>
                  {ICON_NAMES.map((name) => {
                    const IconComp = ICONS_MAP[name];
                    return (
                      <SelectItem key={name} value={name}>
                        <div className="flex items-center gap-2">
                          <IconComp className="h-4 w-4" />
                          <span>{name}</span>
                        </div>
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
             <DialogClose asChild>
                <Button type="button" variant="outline" onClick={closeDialog}>Cancel</Button>
            </DialogClose>
            <Button type="submit" onClick={handleAddOrEditCategory}>
              <Save className="mr-2 h-4 w-4" /> {editingCategory ? 'Save Changes' : 'Add Category'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
