# Reference existing VM managed by ParkFlow Terraform (to avoid state conflicts)
data "google_compute_instance" "parkflow_vm" {
  name = var.vm_instance_name
  zone = var.zone
}

# Persistent Disk Snapshot Schedule (Daily Backups for MilHub safety)
resource "google_compute_resource_policy" "daily_snapshot" {
  name   = "milhub-daily-vm-snapshot-policy"
  region = var.region

  snapshot_schedule_policy {
    schedule {
      daily_schedule {
        days_in_cycle = 1
        start_time    = "03:00"
      }
    }

    retention_policy {
      max_retention_days    = 7
      on_source_disk_delete = "KEEP_AUTO_SNAPSHOTS"
    }

    snapshot_properties {
      labels = {
        environment = "production"
        managed_by  = "milhub-terraform"
      }
    }
  }
}
