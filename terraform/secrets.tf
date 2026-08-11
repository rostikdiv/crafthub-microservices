# Secret Manager Entries for Production Credentials

resource "google_secret_manager_secret" "db_password" {
  secret_id = "milhub-db-password"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "milhub-jwt-secret"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}

resource "google_secret_manager_secret" "mongo_uri" {
  secret_id = "milhub-mongo-uri"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}

resource "google_secret_manager_secret" "gcs_hmac_access_key" {
  secret_id = "gcs-hmac-access-key"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}

resource "google_secret_manager_secret" "gcs_hmac_secret_key" {
  secret_id = "gcs-hmac-secret-key"
  replication {
    user_managed {
      replicas {
        location = var.region
      }
    }
  }
}
