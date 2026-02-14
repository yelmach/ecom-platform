export interface MediaImage {
  id: string;
  url: string;
}

export interface ProductImagesResponse {
  productId: string;
  images: MediaImage[];
}

export interface ProfileImageResponse {
  avatar: MediaImage;
}
