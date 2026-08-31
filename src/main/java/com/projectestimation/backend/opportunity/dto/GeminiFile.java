package com.projectestimation.backend.opportunity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GeminiFile {
	private String fileUri;
	private String mimeType;

}
