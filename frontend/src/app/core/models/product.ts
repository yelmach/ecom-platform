export interface Product {
    id: String;
    name: String;
    description: String;
    price: number;
    quantity: number;
    mediaIds : String[];
    sellerId: String;
    createdAt: Date;
    updatedAt: Date;
}

export interface ProductFormData {
    name : String;
    description: String;
    price: number;
    quantity : number;
    mediaIds : String[];
}

export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}