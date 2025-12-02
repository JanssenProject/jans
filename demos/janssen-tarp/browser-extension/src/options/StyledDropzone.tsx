import React, { useEffect, useCallback, useState } from 'react';
import { useDropzone } from 'react-dropzone';
import styled from 'styled-components';

const getColor = (props) => {
    if (props.isDragAccept) {
        return '#00e676';
    }
    if (props.isDragReject) {
        return '#ff1744';
    }
    if (props.isFocused) {
        return '#2196f3';
    }
    return '#eeeeee';
}

const Container = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-width: 2px;
  border-radius: 2px;
  border-color: ${props => getColor(props)};
  border-style: dashed;
  background-color: #fafafa;
  color: #bdbdbd;
  outline: none;
  transition: border .24s ease-in-out;
  margin-bottom: 30px;
`;

function StyledDropzone(props) {

    const [selectedFileName, setSelectedFileName] = useState(null)
    const [selectedFile, setSelectedFile] = useState(null)

    useEffect(() => {
        if (selectedFile) {
            props.submitFile(selectedFile)
        }
    }, [selectedFile])

    const onDrop = useCallback(acceptedFiles => {
        const file = acceptedFiles[0]
        setSelectedFileName(file.name)
        setSelectedFile(file)
    }, [])
    const { getRootProps,
        getInputProps,
        isFocused,
        isDragAccept,
        isDragReject } = useDropzone({
            onDrop,
            accept: {
                'application/jwt': ['.jwt'],
            },
        })


    return (
        <div>
            <Container {...getRootProps({ isFocused, isDragAccept, isDragReject })}>
                <input {...getInputProps()} />
                {selectedFileName ? (
                    <strong>Selected File : {selectedFileName}</strong>
                ) : (
                    <p>Drag &apos;n&apos; drop .jwt file here, or click to select file</p>
                )}
            </Container>
        </div>
    );
}

export default StyledDropzone;