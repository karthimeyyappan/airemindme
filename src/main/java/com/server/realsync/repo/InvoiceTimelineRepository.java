package com.server.realsync.repo;

/**
 * Placeholder – timeline events are NOT stored in a separate table.
 * They live as a JSON array in {@code invoices.timeline_json} on the
 * existing Invoice entity.
 *
 * All timeline read/write logic is handled inside
 * {@link com.server.realsync.services.InvoiceTimelineService}.
 *
 * This file is intentionally kept empty so that no code needs to be
 * deleted and the package structure stays consistent.
 */
public final class InvoiceTimelineRepository {
    // No JPA repository needed – using Invoice entity directly.
    private InvoiceTimelineRepository() {}
}
