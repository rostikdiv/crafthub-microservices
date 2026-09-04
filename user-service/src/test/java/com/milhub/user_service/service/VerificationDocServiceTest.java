package com.milhub.user_service.service;

import com.milhub.user_service.dto.admin.VerificationResponseDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.VerificationDoc;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.repository.VerificationDocRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationDocServiceTest {

    @Mock
    private VerificationDocRepository docRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private VerificationDocService verificationDocService;

    private UUID currentUserId;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        currentUser = User.builder()
                .id(currentUserId)
                .email("soldier@milhub.ua")
                .firstName("Ivan")
                .lastName("Koval")
                .role(Role.BUYER)
                .isVerified(false)
                .createdAt(Timestamp.from(Instant.now()))
                .documents(new ArrayList<>())
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(currentUserId.toString());
        SecurityContextHolder.setContext(securityContext);

        ReflectionTestUtils.setField(verificationDocService, "documentsBucket", "documents");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw BusinessException when document limit (10) is exceeded")
    void uploadDocument_WhenLimitExceeded_ShouldThrowException() {
        for (int i = 0; i < 10; i++) {
            currentUser.getDocuments().add(new VerificationDoc());
        }
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        VerificationDocRequestDTO dto = new VerificationDocRequestDTO(DocumentType.MILITARY_ID, "http://minio/doc.pdf");

        assertThatThrownBy(() -> verificationDocService.uploadDocument(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limit of documents exceeded");

        verify(docRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should upload document with PENDING status successfully")
    void uploadDocument_Success_ShouldSaveWithPendingStatus() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        UUID docId = UUID.randomUUID();
        VerificationDoc savedDoc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .documentType(DocumentType.MILITARY_ID)
                .docUrl("http://minio/doc.pdf")
                .status(VerificationStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(docRepository.save(any(VerificationDoc.class))).thenReturn(savedDoc);

        VerificationDocRequestDTO dto = new VerificationDocRequestDTO(DocumentType.MILITARY_ID, "http://minio/doc.pdf");
        VerificationResponseDTO response = verificationDocService.uploadDocument(dto);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(docId);
        assertThat(response.documentType()).isEqualTo(DocumentType.MILITARY_ID);
        assertThat(response.status()).isEqualTo(VerificationStatus.PENDING);

        ArgumentCaptor<VerificationDoc> captor = ArgumentCaptor.forClass(VerificationDoc.class);
        verify(docRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    @DisplayName("Should retrieve documents belonging to current user")
    void getMyDocuments_ShouldReturnUserDocuments() {
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        VerificationDoc doc = VerificationDoc.builder()
                .id(UUID.randomUUID())
                .user(currentUser)
                .documentType(DocumentType.UNIT_ORDER)
                .docUrl("http://minio/order.pdf")
                .status(VerificationStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(docRepository.findAllByUserId(currentUserId)).thenReturn(List.of(doc));

        List<VerificationResponseDTO> result = verificationDocService.getMyDocuments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(doc.getId());
        assertThat(result.get(0).documentType()).isEqualTo(DocumentType.UNIT_ORDER);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent document")
    void deleteDocument_WhenNotFound_ShouldThrowException() {
        UUID docId = UUID.randomUUID();
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(docRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationDocService.deleteDocument(docId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trying to delete another user's document")
    void deleteDocument_WhenNotOwner_ShouldThrowException() {
        UUID docId = UUID.randomUUID();
        User anotherUser = User.builder().id(UUID.randomUUID()).build();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(anotherUser)
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> verificationDocService.deleteDocument(docId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("access denied");

        verify(docRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when deleting already APPROVED document")
    void deleteDocument_WhenApproved_ShouldThrowException() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .status(VerificationStatus.APPROVED)
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> verificationDocService.deleteDocument(docId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot delete approved document");

        verify(docRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete document successfully when user is owner and status is PENDING")
    void deleteDocument_Success_ShouldDelete() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));

        verificationDocService.deleteDocument(docId);

        verify(docRepository).delete(doc);
    }

    @Test
    @DisplayName("Admin: Should throw ResourceNotFoundException when user does not exist for getDocumentsByUserId")
    void getDocumentsByUserId_WhenUserDoesNotExist_ShouldThrowException() {
        UUID targetUserId = UUID.randomUUID();
        when(userRepository.existsById(targetUserId)).thenReturn(false);

        assertThatThrownBy(() -> verificationDocService.getDocumentsByUserId(targetUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Admin: Should update document status to APPROVED")
    void updateDocumentStatus_WhenApproved_ShouldSetApproved() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .status(VerificationStatus.PENDING)
                .build();

        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));

        verificationDocService.updateDocumentStatus(docId, true);

        assertThat(doc.getStatus()).isEqualTo(VerificationStatus.APPROVED);
        verify(docRepository).save(doc);
    }

    @Test
    @DisplayName("Admin: Should update document status to REJECTED")
    void updateDocumentStatus_WhenRejected_ShouldSetRejected() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .status(VerificationStatus.PENDING)
                .build();

        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));

        verificationDocService.updateDocumentStatus(docId, false);

        assertThat(doc.getStatus()).isEqualTo(VerificationStatus.REJECTED);
        verify(docRepository).save(doc);
    }

    @Test
    @DisplayName("Download: Should allow document owner to download")
    void downloadDocument_WhenOwner_ShouldReturnDownloadDTO() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .docUrl("http://localhost:9000/documents/test-doc.pdf")
                .status(VerificationStatus.APPROVED)
                .build();

        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(fileStorageService.extractObjectNameFromUrl("http://localhost:9000/documents/test-doc.pdf", "documents"))
                .thenReturn("test-doc.pdf");

        InputStream fakeStream = new ByteArrayInputStream("fake-pdf-content".getBytes());
        when(fileStorageService.getFile("documents", "test-doc.pdf")).thenReturn(fakeStream);

        VerificationDocService.DocumentDownloadDTO downloadDTO = verificationDocService.downloadDocument(docId);

        assertThat(downloadDTO).isNotNull();
        assertThat(downloadDTO.filename()).isEqualTo("test-doc.pdf");
        assertThat(downloadDTO.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("downloadDocument: allows ADMIN to download non-owned document")
    void downloadDocument_WhenAdmin_ShouldAllowDownload() {
        UUID docId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(otherUser)
                .docUrl("http://localhost:9000/documents/other.pdf")
                .build();

        currentUser.setRole(Role.ADMIN);
        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(fileStorageService.extractObjectNameFromUrl("http://localhost:9000/documents/other.pdf", "documents"))
                .thenReturn("other.pdf");
        when(fileStorageService.getFile("documents", "other.pdf"))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

        VerificationDocService.DocumentDownloadDTO downloadDTO = verificationDocService.downloadDocument(docId);

        assertThat(downloadDTO).isNotNull();
        assertThat(downloadDTO.filename()).isEqualTo("other.pdf");
    }

    @Test
    @DisplayName("downloadDocument: throws BusinessException when user is neither owner nor admin")
    void downloadDocument_WhenNeitherOwnerNorAdmin_ShouldThrowAccessDenied() {
        UUID docId = UUID.randomUUID();
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(otherUser)
                .docUrl("http://localhost:9000/documents/other.pdf")
                .build();

        currentUser.setRole(Role.BUYER);
        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));

        assertThatThrownBy(() -> verificationDocService.downloadDocument(docId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("downloadDocument: throws ResourceNotFoundException when storage cannot extract object name")
    void downloadDocument_WhenObjectNameNull_ShouldThrowException() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .docUrl("invalid-url")
                .build();

        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(fileStorageService.extractObjectNameFromUrl("invalid-url", "documents")).thenReturn(null);

        assertThatThrownBy(() -> verificationDocService.downloadDocument(docId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found in storage");
    }

    @Test
    @DisplayName("downloadDocument: handles extensionless file with default octet-stream")
    void downloadDocument_WhenNoExtension_ShouldDetermineDefaultContentType() {
        UUID docId = UUID.randomUUID();
        VerificationDoc doc = VerificationDoc.builder()
                .id(docId)
                .user(currentUser)
                .docUrl("http://localhost:9000/documents/rawfile")
                .build();

        when(docRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(fileStorageService.extractObjectNameFromUrl("http://localhost:9000/documents/rawfile", "documents"))
                .thenReturn("rawfile");
        when(fileStorageService.getFile("documents", "rawfile"))
                .thenReturn(new ByteArrayInputStream("data".getBytes()));

        VerificationDocService.DocumentDownloadDTO downloadDTO = verificationDocService.downloadDocument(docId);

        assertThat(downloadDTO.contentType()).isEqualTo("application/octet-stream");
    }
}
