package com.example.community.repository.main.post.report;

import com.example.community.entity.main.post.report.PostReport;
import com.example.community.entity.main.post.report.PostReportId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, PostReportId> {
}