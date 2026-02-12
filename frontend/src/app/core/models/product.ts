export interface Product {
    id: String;
    name: String;
    description: String;
    price: number;
    quantity: number;
    sellerId: String;
    createdAt: Date;
    updatedAt: Date;
}

export interface ProductFormData {
    name : String;
    description: String;
    pirce: number;
    quantity : number;
}