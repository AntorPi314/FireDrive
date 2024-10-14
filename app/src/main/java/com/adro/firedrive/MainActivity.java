package com.adro.firedrive;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.provider.OpenableColumns;
import java.util.Map;
import com.google.firebase.storage.StorageException;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILES_REQUEST_CODE = 1;
    public RecyclerView recyclerView;
    private FileAdapter adapter;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    public List<FileModel> fileList = new ArrayList<>();
    public List<Integer> selectedItems = new ArrayList<>();
    private TextView tvProcess;
    public Button btnUpload;
    private int totalFiles;
    private int uploadedFiles;

    private static final int PAGE_SIZE = 15;
    public DocumentSnapshot lastVisible = null;
    public boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        recyclerView = findViewById(R.id.recyclerview1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileAdapter(fileList, selectedItems, this::onFileLongClick, this::onFileClick, this);
        recyclerView.setAdapter(adapter);

        tvProcess = findViewById(R.id.tvProcess);
        btnUpload = findViewById(R.id.btnUpload);

        btnUpload.setOnClickListener(v -> selectFilesForUpload());

        loadFilesFromFirestore();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                assert layoutManager != null;
                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition + 1 >= totalItemCount && !isLoading) {
                    loadFilesFromFirestore();
                }
            }
        });
    }

    private void selectFilesForUpload() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_FILES_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILES_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                List<Uri> uriList = new ArrayList<>();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri fileUri = data.getClipData().getItemAt(i).getUri();
                        uriList.add(fileUri);
                    }
                } else if (data.getData() != null) {
                    Uri fileUri = data.getData();
                    uriList.add(fileUri);
                }
                uploadFiles(uriList);
            }
        }
    }

    private void uploadFiles(List<Uri> uriList) {
        totalFiles = uriList.size();
        uploadedFiles = 0;
        tvProcess.setText(String.format("0/%d  |  0%%", totalFiles));
        for (Uri fileUri : uriList) {
            uploadFileToFirebase(fileUri);
        }
    }

    private void uploadFileToFirebase(Uri fileUri) {
        String fileName = getFileName(fileUri);
        StorageReference storageRef = storage.getReference().child("Upload/" + fileName); // Upload folder

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        saveFileDataToFirestore(fileName, downloadUrl, taskSnapshot.getTotalByteCount(), fileUri);
                    });
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to upload: " + fileName, Toast.LENGTH_SHORT).show())
                .addOnProgressListener(snapshot -> {
                    long progress = (100 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    tvProcess.setText(String.format("%d/%d  |  %d%%", uploadedFiles, totalFiles, progress));
                });
    }

    private void saveFileDataToFirestore(String fileName, String fileUrl, long fileSizeBytes, Uri fileUri) {
        double fileSizeInKB = fileSizeBytes / 1024.0;
        String date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());

        FileModel file = new FileModel();
        file.setFileName(fileName);
        file.setFileUrl(fileUrl);
        file.setSize(fileSizeInKB);
        file.setDate(date);
        file.setTime(time);

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("fileName", fileName);
        fileData.put("fileUrl", fileUrl);
        fileData.put("size", fileSizeInKB);
        fileData.put("date", date);
        fileData.put("time", time);
        fileData.put("timestamp", FieldValue.serverTimestamp()); // Set the Firestore timestamp

        fileList.add(0, file);
        adapter.notifyDataSetChanged();

        firestore.collection("files").add(fileData)
                .addOnSuccessListener(documentReference -> {
                    uploadedFiles++;
                    tvProcess.setText(String.format("%d/%d  |  100%%", uploadedFiles, totalFiles));
                    if (uploadedFiles == totalFiles) {
                        Toast.makeText(MainActivity.this, "All files uploaded successfully", Toast.LENGTH_SHORT).show();
                        loadFilesFromFirestore();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to save file info", Toast.LENGTH_SHORT).show());
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private void loadFilesFromFirestore() {
        if (isLoading) return;
        isLoading = true;

        Query query;
        if (lastVisible == null) {
            // Initial load (first batch)
            query = firestore.collection("files")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);
        } else {
            query = firestore.collection("files")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(PAGE_SIZE);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().size() > 0) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        FileModel file = document.toObject(FileModel.class);
                        fileList.add(file);
                    }
                    adapter.notifyDataSetChanged();

                    lastVisible = task.getResult().getDocuments()
                            .get(task.getResult().size() - 1);
                } else {
                   // Toast.makeText(MainActivity.this, "No more files to load", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Error loading files", Toast.LENGTH_SHORT).show();
            }
            isLoading = false;
        });
    }

    private void onFileLongClick(int position) {
        if (!selectedItems.isEmpty()) {
            showDeleteConfirmationDialog();
        }
    }

    private void onFileClick(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(Integer.valueOf(position));
        } else {
            selectedItems.add(position);
        }
        adapter.notifyItemChanged(position);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Files")
                .setMessage("Are you sure you want to delete the selected files?")
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedFiles())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteSelectedFiles() {
        List<FileModel> filesToDelete = new ArrayList<>();
        for (int position : selectedItems) {
            filesToDelete.add(fileList.get(position));
        }

        for (FileModel file : filesToDelete) {
            StorageReference fileRef = storage.getReferenceFromUrl(file.getFileUrl());

            fileRef.delete().addOnSuccessListener(aVoid -> {
                firestore.collection("files").whereEqualTo("fileUrl", file.getFileUrl())
                        .get().addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    firestore.collection("files").document(document.getId())
                                            .delete()
                                            .addOnSuccessListener(aVoid1 -> {
                                                fileList.remove(file);
                                                adapter.notifyDataSetChanged();
                                               // Toast.makeText(MainActivity.this, "File deleted from Firestore and Firebase Storage", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                              //  Toast.makeText(MainActivity.this, "Failed to delete file from Firestore", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "File not found in Firestore", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Error fetching file from Firestore", Toast.LENGTH_SHORT).show();
                        });
            }).addOnFailureListener(e -> {
                if (((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    firestore.collection("files").whereEqualTo("fileUrl", file.getFileUrl())
                            .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        firestore.collection("files").document(document.getId())
                                                .delete()
                                                .addOnSuccessListener(aVoid1 -> {
                                                    fileList.remove(file);
                                                    adapter.notifyDataSetChanged();
                                                    Toast.makeText(MainActivity.this, "Deleted missing file from Firestore", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                }
                            });
                } else {
                    Toast.makeText(MainActivity.this, "Error deleting file from Firebase Storage: " + file.getFileName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        selectedItems.clear();
    }

    @Override
    public void onBackPressed() {
        if (!selectedItems.isEmpty()) {
            selectedItems.clear();
            adapter.resetAllURLs();
            adapter.notifyDataSetChanged();
        } else {
            super.onBackPressed();
        }
    }
}
