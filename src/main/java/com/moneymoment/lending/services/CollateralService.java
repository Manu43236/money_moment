package com.moneymoment.lending.services;

import com.moneymoment.lending.common.constants.AppConstants;
import com.moneymoment.lending.common.enums.CollateralTypesEnum;
import com.moneymoment.lending.common.enums.LoanStatusEnums;
import com.moneymoment.lending.common.exception.BusinessLogicException;
import com.moneymoment.lending.common.exception.ResourceNotFoundException;
import com.moneymoment.lending.common.exception.ValidationException;
import com.moneymoment.lending.common.utils.NumberGenerator;
import com.moneymoment.lending.dtos.CollateralRequestDto;
import com.moneymoment.lending.dtos.CollateralResponseDto;
import com.moneymoment.lending.entities.CollateralDetailsEntity;
import com.moneymoment.lending.entities.LoanEntity;
import com.moneymoment.lending.repos.CollateralDetailsRepository;
import com.moneymoment.lending.repos.LoanRepo;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CollateralService {

    private LoanRepo loanRepo;
    private CollateralDetailsRepository collateralDetailsRepository;

    CollateralService(LoanRepo loanRepo, CollateralDetailsRepository collateralDetailsRepository) {
        this.loanRepo = loanRepo;
        this.collateralDetailsRepository = collateralDetailsRepository;

    }

    @Transactional
    public CollateralResponseDto registerCollateral(CollateralRequestDto request) {
        LoanEntity loan = loanRepo.findByLoanNumber(request.getLoanNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", request.getLoanNumber()));

        if (!loan.getLoanType().getCollateralRequired()) {
            throw new BusinessLogicException(
                    "Loan type " + loan.getLoanType().getName() + " does not require collateral");
        }

        if (!loan.getLoanType().getCollateralType().equals(request.getCollateralType())) {
            throw new ValidationException(
                    "Invalid collateral type. Expected: " + loan.getLoanType().getCollateralType()
                            + ", Got: " + request.getCollateralType());
        }

        Optional<CollateralDetailsEntity> existing = collateralDetailsRepository.findByLoanId(loan.getId());

        if (existing.isPresent()) {
            throw new BusinessLogicException(
                    "Collateral already registered for this loan. Collateral number: "
                            + existing.get().getCollateralNumber());
        }

        Double valuationAmount = request.getValuationAmount();
        Double loanAmount = loan.getLoanAmount();
        Double ltvPercentage = (loanAmount / valuationAmount) * 100;

        // Round to 2 decimal places
        ltvPercentage = Math.round(ltvPercentage * 100.0) / 100.0;

        Double maxAllowedLtv = loan.getLoanType().getMaxLtvPercentage();

        if (ltvPercentage > maxAllowedLtv) {
            throw new BusinessLogicException(
                    "LTV ratio " + ltvPercentage + "% exceeds maximum allowed " + maxAllowedLtv
                            + "%. Either reduce loan amount or increase collateral value.");
        }

        CollateralDetailsEntity collateral = new CollateralDetailsEntity();

        // Generate collateral number
        collateral.setCollateralNumber(
                NumberGenerator.numberGeneratorWithPrifix(AppConstants.COLLATERAL_NUMBER_PREFIX));

        // Relationships
        collateral.setLoan(loan);
        collateral.setCustomer(loan.getCustomer());

        // Collateral Type
        collateral.setCollateralType(request.getCollateralType());

        // Set type-specific fields based on collateral type
        if (request.getCollateralType().equals(CollateralTypesEnum.PROPERTY)) {
            collateral.setPropertyAddress(request.getPropertyAddress());
            collateral.setPropertyType(request.getPropertyType());
            collateral.setPropertyAreaSqft(request.getPropertyAreaSqft());
            collateral.setPropertyValue(request.getPropertyValue());

        } else if (request.getCollateralType().equals(CollateralTypesEnum.VEHICLE)) {
            collateral.setVehicleRegistrationNumber(request.getVehicleRegistrationNumber());
            collateral.setVehicleMake(request.getVehicleMake());
            collateral.setVehicleModel(request.getVehicleModel());
            collateral.setVehicleYear(request.getVehicleYear());
            collateral.setVehicleValue(request.getVehicleValue());

        } else if (request.getCollateralType().equals(CollateralTypesEnum.GOLD)) {
            collateral.setGoldWeightGrams(request.getGoldWeightGrams());
            collateral.setGoldPurity(request.getGoldPurity());
            collateral.setGoldItemDescription(request.getGoldItemDescription());
            collateral.setGoldValue(request.getGoldValue());
        }

        // Valuation
        collateral.setValuationAmount(valuationAmount);
        collateral.setValuationDate(request.getValuationDate());
        collateral.setValuatorName(request.getValuatorName());
        collateral.setValuatorCertificateNumber(request.getValuatorCertificateNumber());

        // LTV
        collateral.setLoanAmount(loanAmount);
        collateral.setLtvPercentage(ltvPercentage);

        // Status
        collateral.setCollateralStatus("PLEDGED");
        collateral.setPledgeDate(LocalDate.now());

        // Remarks
        collateral.setRemarks(request.getRemarks());

        collateral = collateralDetailsRepository.save(collateral);

        return toDto(collateral);

    }

    private CollateralResponseDto toDto(CollateralDetailsEntity entity) {
        CollateralResponseDto dto = new CollateralResponseDto();

        // Identifiers
        dto.setId(entity.getId());
        dto.setCollateralNumber(entity.getCollateralNumber());

        // Loan Info
        dto.setLoanId(entity.getLoan().getId());
        dto.setLoanNumber(entity.getLoan().getLoanNumber());
        dto.setLoanAmount(entity.getLoanAmount());

        // Customer Info
        dto.setCustomerId(entity.getCustomer().getId());
        dto.setCustomerNumber(entity.getCustomer().getCustomerNumber());
        dto.setCustomerName(entity.getCustomer().getName());

        // Collateral Type
        dto.setCollateralType(entity.getCollateralType());

        // Property Details
        dto.setPropertyAddress(entity.getPropertyAddress());
        dto.setPropertyType(entity.getPropertyType());
        dto.setPropertyAreaSqft(entity.getPropertyAreaSqft());
        dto.setPropertyValue(entity.getPropertyValue());

        // Vehicle Details
        dto.setVehicleRegistrationNumber(entity.getVehicleRegistrationNumber());
        dto.setVehicleMake(entity.getVehicleMake());
        dto.setVehicleModel(entity.getVehicleModel());
        dto.setVehicleYear(entity.getVehicleYear());
        dto.setVehicleValue(entity.getVehicleValue());

        // Gold Details
        dto.setGoldWeightGrams(entity.getGoldWeightGrams());
        dto.setGoldPurity(entity.getGoldPurity());
        dto.setGoldItemDescription(entity.getGoldItemDescription());
        dto.setGoldValue(entity.getGoldValue());

        // Valuation
        dto.setValuationAmount(entity.getValuationAmount());
        dto.setValuationDate(entity.getValuationDate());
        dto.setValuatorName(entity.getValuatorName());
        dto.setValuatorCertificateNumber(entity.getValuatorCertificateNumber());

        // LTV
        dto.setLtvPercentage(entity.getLtvPercentage());
        dto.setMaxAllowedLtv(entity.getLoan().getLoanType().getMaxLtvPercentage());

        // Status
        dto.setCollateralStatus(entity.getCollateralStatus());
        dto.setPledgeDate(entity.getPledgeDate());
        dto.setReleaseDate(entity.getReleaseDate());

        // Remarks
        dto.setRemarks(entity.getRemarks());

        // Audit
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    public CollateralResponseDto getCollateralByLoan(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        CollateralDetailsEntity collateral = collateralDetailsRepository.findByLoanId(loan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No collateral found for loan: " + loanNumber));

        return toDto(collateral);
    }

    public CollateralResponseDto releaseCollateral(String loanNumber) {
        LoanEntity loan = loanRepo.findByLoanNumber(loanNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "loanNumber", loanNumber));

        // Check loan is fully paid or closed
        if (!loan.getLoanStatus().getCode().equals(LoanStatusEnums.CLOSED)) {
            throw new BusinessLogicException(
                    "Collateral can only be released for closed loans. Current status: "
                            + loan.getLoanStatus().getCode());
        }

        CollateralDetailsEntity collateral = collateralDetailsRepository.findByLoanId(loan.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No collateral found for loan: " + loanNumber));

        // Update status
        collateral.setCollateralStatus("RELEASED");
        collateral.setReleaseDate(LocalDate.now());

        collateral = collateralDetailsRepository.save(collateral);

        return toDto(collateral);
    }
}
