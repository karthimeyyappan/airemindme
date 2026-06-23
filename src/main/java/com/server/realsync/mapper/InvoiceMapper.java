package com.server.realsync.mapper;

import com.server.realsync.dto.*;
import com.server.realsync.entity.Invoice;
import com.server.realsync.entity.InvoiceItem;
import com.server.realsync.entity.InvoiceStatus;

import java.util.List;
import java.util.stream.Collectors;

public class InvoiceMapper {

    public static InvoiceListResponseDTO toListDTO(Invoice invoice) {
        InvoiceListResponseDTO dto = new InvoiceListResponseDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerId(invoice.getCustomerId());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setGrandTotal(invoice.getGrandTotal());
        dto.setStatus(invoice.getStatus());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setDueDate(invoice.getDueDate());
        dto.setCustomerPhone(invoice.getCustomerPhone());
        dto.setPaidAmount(invoice.getPaidAmount());
        return dto;
    }

    public static InvoiceDetailResponseDTO toDetailDTO(Invoice invoice) {
        InvoiceDetailResponseDTO dto = new InvoiceDetailResponseDTO();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerId(invoice.getCustomerId());
        dto.setCustomerName(invoice.getCustomerName());
        dto.setCustomerAddress(invoice.getCustomerAddress());
        dto.setCustomerPhone(invoice.getCustomerPhone());
        dto.setCustomerGst(invoice.getCustomerGst());
        dto.setShippingAddress(invoice.getShippingAddress());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setSubtotal(invoice.getSubtotal());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setDiscountAmount(invoice.getDiscountAmount());
        dto.setShippingAmount(invoice.getShippingAmount());
        dto.setGrandTotal(invoice.getGrandTotal());
        dto.setStatus(invoice.getStatus());
        dto.setNotes(invoice.getNotes());
        dto.setPaidAmount(invoice.getPaidAmount());
        dto.setBalanceAmount(invoice.getBalanceAmount());
        dto.setTerms(invoice.getTerms());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        dto.setCreatedBy(invoice.getCreatedBy());
        dto.setUpdatedBy(invoice.getUpdatedBy());

        List<InvoiceItemDTO> items = invoice.getItems().stream().map(it -> {
            InvoiceItemDTO i = new InvoiceItemDTO();
            i.setId(it.getId());
            i.setItemType(it.getItemType());
            i.setItemRefId(it.getItemRefId());
            i.setItemName(it.getItemName());
            i.setDescription(it.getDescription());
            i.setHsnSac(it.getHsnSac());
            i.setQty(it.getQty());
            // InvoiceItem getters return Double for compatibility; convert where possible
            i.setRate(it.getRate());
            i.setGst(it.getGst());
            i.setTaxAmount(it.getTaxAmount());
            i.setLineTotal(it.getLineTotal());
            return i;
        }).collect(Collectors.toList());

        dto.setItems(items);

        return dto;
    }

    public static Invoice toEntity(CreateInvoiceRequestDTO req) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(req.getInvoiceNumber());
        invoice.setCustomerId(req.getCustomerId());
        invoice.setCustomerName(req.getCustomerName());
        invoice.setCustomerAddress(req.getCustomerAddress());
        invoice.setCustomerPhone(req.getCustomerPhone());
        invoice.setCustomerGst(req.getCustomerGst());
        invoice.setShippingAddress(req.getShippingAddress());
        invoice.setInvoiceDate(req.getInvoiceDate());
        invoice.setDueDate(req.getDueDate());
        invoice.setSubtotal(req.getSubtotal() == null ? java.math.BigDecimal.ZERO : req.getSubtotal());
        invoice.setTaxAmount(req.getTaxAmount() == null ? java.math.BigDecimal.ZERO : req.getTaxAmount());
        invoice.setDiscountAmount(
                req.getDiscountAmount() == null ? java.math.BigDecimal.ZERO : req.getDiscountAmount());
        invoice.setShippingAmount(
                req.getShippingAmount() == null ? java.math.BigDecimal.ZERO : req.getShippingAmount());
        invoice.setGrandTotal(req.getGrandTotal() == null ? java.math.BigDecimal.ZERO : req.getGrandTotal());
        invoice.setStatus(req.getStatus() == null ? InvoiceStatus.DRAFT : req.getStatus());
        invoice.setNotes(req.getNotes());
        invoice.setTerms(req.getTerms());

        if (req.getItems() != null) {
            List<InvoiceItem> items = req.getItems().stream().map(it -> {
                InvoiceItem entity = new InvoiceItem();
                entity.setItemType(it.getItemType());
                entity.setItemRefId(it.getItemRefId());
                entity.setItemName(it.getItemName());
                entity.setDescription(it.getDescription());
                entity.setHsnSac(it.getHsnSac());
                entity.setQty(it.getQty());
                entity.setRate(it.getRate());
                entity.setGst(it.getGst());
                entity.setTaxAmount(it.getTaxAmount());
                entity.setLineTotal(it.getLineTotal());
                return entity;
            }).collect(Collectors.toList());

            for (InvoiceItem it : items) {
                it.setInvoice(invoice);
            }

            invoice.setItems(items);
        }

        return invoice;
    }

    public static void updateEntityFromDto(Invoice existing, UpdateInvoiceRequestDTO dto) {
        existing.setInvoiceNumber(dto.getInvoiceNumber());
        existing.setCustomerId(dto.getCustomerId());
        existing.setCustomerName(dto.getCustomerName());
        existing.setCustomerAddress(dto.getCustomerAddress());
        existing.setCustomerPhone(dto.getCustomerPhone());
        existing.setCustomerGst(dto.getCustomerGst());
        existing.setShippingAddress(dto.getShippingAddress());
        existing.setInvoiceDate(dto.getInvoiceDate());
        existing.setDueDate(dto.getDueDate());
        existing.setSubtotal(dto.getSubtotal() == null ? java.math.BigDecimal.ZERO : dto.getSubtotal());
        existing.setTaxAmount(dto.getTaxAmount() == null ? java.math.BigDecimal.ZERO : dto.getTaxAmount());
        existing.setDiscountAmount(
                dto.getDiscountAmount() == null ? java.math.BigDecimal.ZERO : dto.getDiscountAmount());
        existing.setShippingAmount(
                dto.getShippingAmount() == null ? java.math.BigDecimal.ZERO : dto.getShippingAmount());
        existing.setGrandTotal(dto.getGrandTotal() == null ? java.math.BigDecimal.ZERO : dto.getGrandTotal());
        existing.setStatus(dto.getStatus() == null ? InvoiceStatus.DRAFT : dto.getStatus());
        existing.setNotes(dto.getNotes());
        existing.setTerms(dto.getTerms());

        existing.getItems().clear();

        if (dto.getItems() != null) {
            List<InvoiceItem> items = dto.getItems().stream().map(it -> {
                InvoiceItem entity = new InvoiceItem();
                entity.setItemType(it.getItemType());
                entity.setItemRefId(it.getItemRefId());
                entity.setItemName(it.getItemName());
                entity.setDescription(it.getDescription());
                entity.setHsnSac(it.getHsnSac());
                entity.setQty(it.getQty());
                entity.setRate(it.getRate());
                entity.setGst(it.getGst());
                entity.setTaxAmount(it.getTaxAmount());
                entity.setLineTotal(it.getLineTotal());
                entity.setInvoice(existing);
                return entity;
            }).collect(Collectors.toList());

            existing.getItems().addAll(items);
        }
    }
}
