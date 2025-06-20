
import {
  LayoutDashboard,
  FileText,
  PiggyBank,
  Tags,
  ListChecks,
  CreditCard,
  ShoppingCart,
  Home,
  Car,
  Utensils,
  HeartPulse, // Kept for potential use, though not in new default categories
  BookOpen,   // Kept for potential use
  Plane,
  PawPrint,
  Fuel,
  Carrot,
  Cat,
  Shield,
  Store,
  Flame,
  CircleDollarSign,
  Package,
  Lightbulb,
  Wifi,
  PlaySquare,
  Heart,
  Droplet,
  Pill,
  Wrench,
  Stethoscope,
  Gift,
  Landmark,
  Cookie,
  AlertTriangle,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  matchSegments?: number;
}

export const NAV_ITEMS: NavItem[] = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard, matchSegments: 0 },
  { href: '/statements', label: 'Statements', icon: FileText },
  { href: '/budget', label: 'Budget', icon: PiggyBank },
  { href: '/categories', label: 'Categories', icon: Tags },
  { href: '/classify', label: 'Classify', icon: ListChecks },
];

export interface Category {
  id: string;
  name: string;
  icon?: LucideIcon;
}

export const SAMPLE_CATEGORIES: Category[] = [
  { id: 'cat1', name: 'Arriendo', icon: Home },
  { id: 'cat2', name: 'Tarjeta Titular', icon: CreditCard },
  { id: 'cat3', name: 'Vacaciones', icon: Plane },
  { id: 'cat4', name: 'Supermercado', icon: ShoppingCart },
  { id: 'cat5', name: 'Gastos Comunes', icon: FileText },
  { id: 'cat6', name: 'choquito', icon: PawPrint },
  { id: 'cat7', name: 'Bencina', icon: Fuel },
  { id: 'cat8', name: 'veguita', icon: Carrot },
  { id: 'cat9', name: 'Gatos', icon: Cat },
  { id: 'cat10', name: 'Uber', icon: Car },
  { id: 'cat11', name: 'Seguro', icon: Shield },
  { id: 'cat12', name: 'Salir a Comer', icon: Utensils },
  { id: 'cat13', name: 'Almacén', icon: Store },
  { id: 'cat14', name: 'Gas', icon: Flame },
  { id: 'cat15', name: 'Peajes', icon: CircleDollarSign },
  { id: 'cat16', name: 'Delivery', icon: Package },
  { id: 'cat17', name: 'Luz', icon: Lightbulb },
  { id: 'cat18', name: 'Internet', icon: Wifi },
  { id: 'cat19', name: 'Streaming', icon: PlaySquare },
  { id: 'cat20', name: 'bubi', icon: Heart },
  { id: 'cat21', name: 'Agua', icon: Droplet },
  { id: 'cat22', name: 'Farmacia', icon: Pill },
  { id: 'cat23', name: 'Casa', icon: Wrench },
  { id: 'cat24', name: 'Médico', icon: Stethoscope },
  { id: 'cat25', name: 'Regalos', icon: Gift },
  { id: 'cat26', name: 'Crédito', icon: Landmark },
  { id: 'cat27', name: 'Antojos', icon: Cookie },
  { id: 'cat28', name: 'Imprevistos', icon: AlertTriangle },
];

export interface Budget {
  id: string;
  categoryId: string;
  limit: number;
  spent: number;
}

// Note: SAMPLE_BUDGETS may refer to old category IDs and might need manual update
// if consistency with new categories is desired immediately.
export const SAMPLE_BUDGETS: Budget[] = [
  { id: 'bud1', categoryId: 'cat1', limit: 500, spent: 350 }, // Was Groceries, now Arriendo
  { id: 'bud2', categoryId: 'cat2', limit: 200, spent: 180 }, // Was Utilities, now Tarjeta Titular
  { id: 'bud3', categoryId: 'cat3', limit: 150, spent: 100 }, // Was Transport, now Vacaciones
  { id: 'bud4', categoryId: 'cat4', limit: 250, spent: 260 }, // Was Dining Out, now Supermercado
];

export type CardType = 'familia' | 'titular';

export interface Transaction {
  id: string;
  date: string;
  description: string;
  amount: number;
  cardType?: CardType;
  categoryId?: string;
  status: 'categorized' | 'uncategorized';
}

// Note: SAMPLE_TRANSACTIONS may refer to old category IDs and might need manual update
// if consistency with new categories is desired immediately.
export const SAMPLE_TRANSACTIONS: Transaction[] = [
  { id: 'txn1', date: '2024-07-15', description: 'SuperMart Central', amount: 75.50, status: 'uncategorized' },
  { id: 'txn2', date: '2024-07-14', description: 'Gas Station Plus', amount: 45.00, cardType: 'titular', categoryId: 'cat7', status: 'categorized' }, // cat3 was Transport, cat7 is Bencina
  { id: 'txn3', date: '2024-07-13', description: 'The Cozy Cafe', amount: 22.75, status: 'uncategorized' },
  { id: 'txn4', date: '2024-07-12', description: 'Online Course Subscription', amount: 29.99, cardType: 'familia', categoryId: 'cat_old_education', status: 'categorized' }, // cat6 was Education, no direct match, needs re-eval
  { id: 'txn5', date: '2024-07-11', description: 'Electricity Bill', amount: 85.20, cardType: 'titular', categoryId: 'cat17', status: 'categorized' }, // cat2 was Utilities, cat17 is Luz
];

export const CARD_TYPES: { value: CardType; label: string }[] = [
  { value: 'titular', label: 'Titular' },
  { value: 'familia', label: 'Familia' },
];
