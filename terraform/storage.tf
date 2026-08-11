# GCS Buckets for MilHub File Storage

# 1. Products Bucket (Public Read)
resource "google_storage_bucket" "products_bucket" {
  name                     = "${var.project_id}-products-storage"
  location                 = var.region
  force_destroy            = false
  uniform_bucket_level_access = true
}

resource "google_storage_bucket_iam_binding" "products_public_read" {
  bucket = google_storage_bucket.products_bucket.name
  role   = "roles/storage.objectViewer"

  members = [
    "allUsers",
  ]
}

# 2. Avatars Bucket (Public Read)
resource "google_storage_bucket" "avatars_bucket" {
  name                     = "${var.project_id}-avatars-storage"
  location                 = var.region
  force_destroy            = false
  uniform_bucket_level_access = true
}

resource "google_storage_bucket_iam_binding" "avatars_public_read" {
  bucket = google_storage_bucket.avatars_bucket.name
  role   = "roles/storage.objectViewer"

  members = [
    "allUsers",
  ]
}

# 3. Documents Bucket (PRIVATE & PROTECTED)
resource "google_storage_bucket" "documents_bucket" {
  name                     = "${var.project_id}-documents-protected-storage"
  location                 = var.region
  force_destroy            = false
  uniform_bucket_level_access = true
  public_access_prevention = "enforced"
}
