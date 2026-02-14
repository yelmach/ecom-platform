export interface Product {
    id: string;
    name: string;
    description: string;
    price: number;
    quantity: number;
    mediaIds : string[];
    sellerId: string;
    createdAt: Date;
    updatedAt: Date;
}

export interface ProductFormData {
    name : string;
    description: string;
    price: number;
    quantity : number;
    mediaIds : string[];
}

export interface ProductUpdateData {
    name?: string;
    description?: string;
    price?: number;
    quantity?: number;
    mediaIds?: string[];
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    last: boolean;
}
